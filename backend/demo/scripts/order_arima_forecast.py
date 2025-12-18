import argparse
import json
import sys
from dataclasses import asdict, dataclass
from datetime import datetime, date, timedelta
from urllib.parse import urlparse

import numpy as np
import pandas as pd

try:
    import pymysql  # type: ignore
except Exception as e:  # pragma: no cover
    raise RuntimeError(
        "缺少依赖 pymysql，请先安装：pip install -r backend/demo/scripts/requirements.txt"
    ) from e

try:
    from statsmodels.tsa.arima.model import ARIMA  # type: ignore
except Exception as e:  # pragma: no cover
    raise RuntimeError(
        "缺少依赖 statsmodels，请先安装：pip install -r backend/demo/scripts/requirements.txt"
    ) from e


@dataclass
class ForecastPoint:
    date: str  # YYYY-MM-DD
    predicted: int
    lower: int
    upper: int


def parse_mysql_from_jdbc(jdbc_url: str):
    # jdbc:mysql://host:3306/db?params...
    if jdbc_url.startswith("jdbc:"):
        jdbc_url = jdbc_url[len("jdbc:") :]
    u = urlparse(jdbc_url)
    db = (u.path or "/").lstrip("/")
    return {
        "host": u.hostname or "localhost",
        "port": u.port or 3306,
        "db": db,
    }


def fetch_daily_counts(conn, history_days: int):
    # 取 create_time 按天聚合
    sql = """
        SELECT DATE(create_time) AS d, COUNT(*) AS c
        FROM orders
        WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL %s DAY)
        GROUP BY DATE(create_time)
        ORDER BY d ASC
    """
    with conn.cursor() as cur:
        cur.execute(sql, (history_days,))
        rows = cur.fetchall()
    df = pd.DataFrame(rows, columns=["d", "c"])
    if df.empty:
        return pd.DataFrame({"date": [], "count": []})
    df["date"] = pd.to_datetime(df["d"]).dt.date
    df["count"] = df["c"].astype(int)
    return df[["date", "count"]]


def build_series(df, history_days: int):
    today = date.today()
    start = today - timedelta(days=history_days - 1)
    all_days = pd.date_range(start=start, end=today, freq="D")
    s = pd.Series(0, index=all_days)
    if not df.empty:
        for _, r in df.iterrows():
            s[pd.Timestamp(r["date"])] = int(r["count"])
    return s


def arima_forecast(series: pd.Series, steps: int, order=(1, 1, 1), alpha=0.05):
    # series index: DatetimeIndex, values: counts
    y = series.astype(float)
    # 如果全为0或点数太少，直接返回0预测
    if len(y) < 10 or float(y.sum()) == 0.0:
        preds = np.zeros(steps)
        lowers = np.zeros(steps)
        uppers = np.zeros(steps)
        return preds, lowers, uppers, order

    model = ARIMA(y, order=order, enforce_stationarity=False, enforce_invertibility=False)
    fit = model.fit()
    fc = fit.get_forecast(steps=steps)
    mean = fc.predicted_mean.to_numpy()
    ci = fc.conf_int(alpha=alpha).to_numpy()  # shape (steps, 2)
    lower = ci[:, 0]
    upper = ci[:, 1]
    return mean, lower, upper, order


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jdbc-url", required=True)
    ap.add_argument("--username", required=True)
    ap.add_argument("--password", required=True)
    ap.add_argument("--days", type=int, default=3)
    ap.add_argument("--history-days", type=int, default=60)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    info = parse_mysql_from_jdbc(args.jdbc_url)
    conn = pymysql.connect(
        host=info["host"],
        port=info["port"],
        user=args.username,
        password=args.password,
        database=info["db"],
        charset="utf8mb4",
        cursorclass=pymysql.cursors.Cursor,
    )
    try:
        df = fetch_daily_counts(conn, args.history_days)
        series = build_series(df, args.history_days)
        mean, lower, upper, order = arima_forecast(series, args.days, order=(1, 1, 1), alpha=0.05)

        # 未来日期从明天开始
        start = date.today() + timedelta(days=1)
        points = []
        for i in range(args.days):
            d = start + timedelta(days=i)
            p = int(max(0, round(mean[i])))
            lo = int(max(0, round(lower[i])))
            up = int(max(lo, round(upper[i])))
            points.append(ForecastPoint(date=d.isoformat(), predicted=p, lower=lo, upper=up))

        out = {
            "generatedAt": datetime.now().isoformat(timespec="seconds"),
            "historyDays": args.history_days,
            "model": {"type": "ARIMA", "order": list(order), "alpha": 0.05, "confidence": 0.95},
            "history": [
                {"date": d.strftime("%Y-%m-%d"), "count": int(series.loc[d])}
                for d in series.index[-min(len(series.index), 30) :]
            ],
            "forecast": [asdict(p) for p in points],
        }

        with open(args.out, "w", encoding="utf-8") as f:
            json.dump(out, f, ensure_ascii=False, indent=2)
        print(args.out)
    finally:
        conn.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(str(e), file=sys.stderr)
        sys.exit(2)



