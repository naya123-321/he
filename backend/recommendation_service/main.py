from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Dict, List, Optional, Tuple

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field, field_validator
from pydantic.config import ConfigDict

app = FastAPI(title="Pet Memorial Package Recommender", version="1.0.0")

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    # 让调用方能直接看到是否“没带 body / Content-Type 不对”
    raw = await request.body()
    preview = ""
    try:
        preview = raw[:240].decode("utf-8", errors="ignore")
    except Exception:
        preview = ""
    return JSONResponse(
        status_code=422,
        content={
            "detail": exc.errors(),
            "meta": {
                "method": request.method,
                "path": str(request.url.path),
                "contentType": request.headers.get("content-type"),
                "contentLength": request.headers.get("content-length"),
                "rawBodyLen": len(raw) if raw is not None else 0,
                "rawBodyPreview": preview,
            },
        },
    )


class Profile(BaseModel):
    model_config = ConfigDict(extra="ignore")

    petType: str = Field(..., description="宠物类型：cat/dog_small/dog_medium/dog_large/other 或中文")
    # 兼容前端/后端可能传入的 float/str（例如 12.0 / "12"）
    petAge: int = Field(..., ge=0, le=30, description="宠物年龄（岁）")
    deathCause: str = Field(..., description="离世原因：disease/accident/aging/other 或中文")
    budgetMin: Optional[float] = Field(default=None, ge=0, description="预算下限（元）")
    budgetMax: Optional[float] = Field(default=None, ge=0, description="预算上限（元）")
    participants: Optional[int] = Field(default=None, ge=1, le=50, description="参与告别人数")

    @field_validator("petAge", mode="before")
    @classmethod
    def _coerce_age(cls, v):
        if v is None:
            return v
        if isinstance(v, bool):
            return int(v)
        if isinstance(v, (int,)):
            return v
        if isinstance(v, float):
            # 允许 12.0 这种
            return int(round(v))
        if isinstance(v, str):
            s = v.strip()
            if s == "":
                return v
            try:
                return int(float(s))
            except Exception:
                return v
        return v

    @field_validator("budgetMin", "budgetMax", mode="before")
    @classmethod
    def _coerce_budget(cls, v):
        if v is None:
            return None
        if isinstance(v, bool):
            return float(int(v))
        if isinstance(v, (int, float)):
            return float(v)
        if isinstance(v, str):
            s = v.strip()
            if s == "":
                return None
            try:
                return float(s)
            except Exception:
                return None
        return None

    @field_validator("participants", mode="before")
    @classmethod
    def _coerce_participants(cls, v):
        if v is None:
            return None
        if isinstance(v, bool):
            return int(v)
        if isinstance(v, int):
            return v
        if isinstance(v, float):
            return int(round(v))
        if isinstance(v, str):
            s = v.strip()
            if s == "":
                return None
            try:
                return int(float(s))
            except Exception:
                return None
        return None


class RecommendRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")

    profile: Profile
    orders: List[Dict[str, Any]] = Field(default_factory=list, description="历史订单：userId/serviceTypeId/petType/petAge/deathCause")
    serviceTypes: List[Dict[str, Any]] = Field(default_factory=list, description="套餐信息：id/name/duration/price")
    topK: int = Field(default=256, ge=1, le=2000)


class RecommendResponse(BaseModel):
    model_config = ConfigDict(extra="ignore")

    recommendedServiceTypeId: Optional[int] = None
    score: float = 0
    similarUsers: int = 0
    analysis: List[str] = Field(default_factory=list)
    algorithm: str = ""
    warning: str = ""


@app.get("/health")
def health():
    return {"ok": True}


def _norm_pet_type(v: str) -> str:
    s = (v or "").strip().lower()
    mapping = {
        "猫": "cat",
        "cat": "cat",
        "小型犬": "dog_small",
        "dog_small": "dog_small",
        "中型犬": "dog_medium",
        "dog_medium": "dog_medium",
        "大型犬": "dog_large",
        "dog_large": "dog_large",
        "犬": "dog",
        "dog": "dog",
    }
    return mapping.get(s, s or "other")


def _pet_type_text(code: str) -> str:
    code = _norm_pet_type(code)
    if code == "cat":
        return "猫"
    if code == "dog_small":
        return "小型犬"
    if code == "dog_medium":
        return "中型犬"
    if code == "dog_large":
        return "大型犬"
    if code.startswith("dog"):
        return "犬"
    return "其他"


def _norm_cause(v: str) -> str:
    s = (v or "").strip().lower()
    mapping = {
        "疾病": "disease",
        "disease": "disease",
        "意外": "accident",
        "accident": "accident",
        "自然衰老": "aging",
        "衰老": "aging",
        "aging": "aging",
    }
    return mapping.get(s, s or "other")


def _cause_text(code: str) -> str:
    code = _norm_cause(code)
    if code == "disease":
        return "疾病"
    if code == "accident":
        return "意外"
    if code == "aging":
        return "自然衰老"
    return "其他"


def _age_bucket(age: int) -> int:
    if age <= 1:
        return 0
    if age <= 5:
        return 1
    if age <= 10:
        return 2
    return 3


def _user_profile_from_orders(rows: List[Dict[str, Any]]) -> Tuple[str, int, str]:
    # 取该用户最近一次出现的特征（简单）
    pet_type = ""
    pet_age = 0
    cause = ""
    for r in rows[::-1]:
        pet_type = r.get("petType") or pet_type
        pet_age = r.get("petAge") if r.get("petAge") is not None else pet_age
        cause = r.get("deathCause") or cause
        if pet_type and cause:
            break
    return _norm_pet_type(str(pet_type)), int(pet_age or 0), _norm_cause(str(cause))


def _similarity(target: Profile, user_pt: str, user_age: int, user_cause: str) -> float:
    # 特征匹配加权（0~1）
    t_pt = _norm_pet_type(target.petType)
    t_c = _norm_cause(target.deathCause)
    t_ab = _age_bucket(target.petAge)

    u_ab = _age_bucket(user_age)

    score = 0.0
    if t_pt and user_pt and t_pt == user_pt:
        score += 0.5
    if t_c and user_cause and t_c == user_cause:
        score += 0.3
    if t_ab == u_ab:
        score += 0.2
    return score


def _rule_score(target: Profile, st: Dict[str, Any]) -> float:
    # 0~1
    duration = st.get("duration")
    price = st.get("price")
    try:
        duration = int(duration) if duration is not None else None
    except Exception:
        duration = None
    try:
        price = float(price) if price is not None else None
    except Exception:
        price = None

    pt = _norm_pet_type(target.petType)
    cause = _norm_cause(target.deathCause)
    age = int(target.petAge or 0)

    score = 0.2

    # 宠物体型/类型 -> 时长偏好
    if duration is not None:
        if pt == "dog_large" and duration >= 90:
            score += 0.25
        elif pt == "dog_medium" and duration >= 75:
            score += 0.22
        elif pt in ("dog_small", "cat") and duration <= 75:
            score += 0.20
        else:
            score += 0.10
    else:
        score += 0.08

    # 年龄 -> 更偏完整流程（价格/时长粗略近似）
    if age >= 8:
        if (price is not None and price >= 0) or (duration is not None and duration >= 75):
            score += 0.18
    elif age <= 2:
        score += 0.12
    else:
        score += 0.10

    # 离世原因 -> 疾病偏温馨
    if cause == "disease":
        score += 0.15
    elif cause == "accident":
        score += 0.10
    else:
        score += 0.08

    # 预算匹配（尽量不超预算）
    if price is not None and (target.budgetMin is not None or target.budgetMax is not None):
        bmin = target.budgetMin
        bmax = target.budgetMax
        # 兼容错误输入：max < min
        if bmin is not None and bmax is not None and bmax < bmin:
            bmin, bmax = bmax, bmin

        # 目标价：偏向预算上限的“中高档”，避免预算越高越选最便宜
        if bmin is not None and bmax is not None:
            span = max(1.0, float(bmax) - float(bmin))
            target_price = float(bmin) + 0.70 * span
            # 超预算强惩罚；低于预算下限按距离惩罚；预算内按贴近目标加分
            if price > float(bmax):
                over_ratio = (price - float(bmax)) / max(1.0, float(bmax))
                score -= min(0.55, 0.30 + 0.60 * over_ratio)
            elif price < float(bmin):
                under_ratio = (float(bmin) - price) / max(1.0, float(bmin))
                score -= min(0.35, 0.10 + 0.35 * under_ratio)
            else:
                dist = abs(price - target_price)
                scale = max(1.0, 0.70 * span)
                closeness = max(0.0, 1.0 - min(1.0, dist / scale))
                score += 0.10 + 0.18 * closeness
        elif bmax is not None:
            # 只有上限：预算内更偏向靠近上限的方案
            if price > float(bmax):
                over_ratio = (price - float(bmax)) / max(1.0, float(bmax))
                score -= min(0.55, 0.28 + 0.60 * over_ratio)
            else:
                target_price = 0.85 * float(bmax)
                dist = abs(price - target_price)
                scale = max(1.0, 0.85 * float(bmax))
                closeness = max(0.0, 1.0 - min(1.0, dist / scale))
                score += 0.08 + 0.20 * closeness
        else:
            # 只有下限：希望不低于下限，略偏向下限上方的方案
            if price < float(bmin):
                under_ratio = (float(bmin) - price) / max(1.0, float(bmin))
                score -= min(0.35, 0.08 + 0.35 * under_ratio)
            else:
                target_price = 1.30 * float(bmin)
                dist = abs(price - target_price)
                scale = max(1.0, 0.90 * float(bmin))
                closeness = max(0.0, 1.0 - min(1.0, dist / scale))
                score += 0.08 + 0.18 * closeness

    # 人数匹配：人数多更偏向更长时长
    if target.participants is not None and int(target.participants) >= 4:
        if duration is not None and duration >= 90:
            score += 0.12
        elif duration is not None and duration >= 75:
            score += 0.08

    return min(1.0, max(0.0, score))


@app.post("/recommend", response_model=RecommendResponse)
def recommend(req: RecommendRequest) -> RecommendResponse:
    profile = req.profile
    service_types = req.serviceTypes or []
    if not service_types:
        return RecommendResponse(
            recommendedServiceTypeId=None,
            score=0,
            similarUsers=0,
            analysis=["当前暂无可用套餐，请联系管理员"],
            algorithm="推荐服务（无套餐数据）",
            warning="无可用套餐数据",
        )

    # 1) 构建用户订单集合
    by_user: Dict[str, List[Dict[str, Any]]] = {}
    for r in req.orders or []:
        uid = r.get("userId")
        sid = r.get("serviceTypeId")
        if uid is None or sid is None:
            continue
        by_user.setdefault(str(uid), []).append(r)

    # 2) 计算相似用户（基于特征）
    sims: List[Tuple[str, float]] = []
    for uid, rows in by_user.items():
        u_pt, u_age, u_c = _user_profile_from_orders(rows)
        sim = _similarity(profile, u_pt, u_age, u_c)
        if sim > 0:
            sims.append((uid, sim))
    sims.sort(key=lambda x: x[1], reverse=True)
    sims = sims[: int(req.topK or 256)]

    similar_user_ids = {uid for uid, _ in sims}
    similar_users_count = len(similar_user_ids)

    # 3) 协同过滤：相似用户的套餐选择频率（item popularity over similar users）
    freq: Dict[int, int] = {}
    total = 0
    if similar_user_ids:
        for uid in similar_user_ids:
            for r in by_user.get(uid, []):
                sid = r.get("serviceTypeId")
                if sid is None:
                    continue
                try:
                    sid = int(sid)
                except Exception:
                    continue
                freq[sid] = freq.get(sid, 0) + 1
                total += 1

    # 4) 规则引擎：给每个套餐一个规则分
    st_by_id: Dict[int, Dict[str, Any]] = {}
    for st in service_types:
        try:
            stid = int(st.get("id"))
            st_by_id[stid] = st
        except Exception:
            continue

    candidates = list(st_by_id.keys())
    if not candidates:
        return RecommendResponse(
            warning="无有效套餐ID",
            algorithm="推荐服务（无有效套餐ID）",
            analysis=["当前套餐数据异常，请联系管理员"],
        )

    best_id = None
    best_score = -1.0
    has_budget = profile.budgetMin is not None or profile.budgetMax is not None

    # 协同过滤“可信度”：相似用户数量、以及相似用户产生的历史交互总量越高，CF 越可信
    # 数据稀疏时（例如只有 1 个相似用户），应主要依赖规则引擎（尤其预算是强偏好）
    cf_confidence = min(1.0, (similar_users_count / 8.0)) * min(1.0, (total / 20.0))
    base_cf_weight = 0.35 if has_budget else 0.70
    cf_weight = base_cf_weight * cf_confidence
    rule_weight = 1.0 - cf_weight

    for sid in candidates:
        cf = (freq.get(sid, 0) / total) if total > 0 else 0.0  # 0~1
        rule = _rule_score(profile, st_by_id[sid])  # 0~1
        price = st_by_id[sid].get("price")
        try:
            price = float(price) if price is not None else None
        except Exception:
            price = None

        final = cf_weight * cf + rule_weight * rule
        if has_budget and price is not None and profile.budgetMax is not None and price > float(profile.budgetMax):
            # 超预算：直接大幅降低，让“再热门也别推荐超预算”
            final -= 0.60
        if final > best_score:
            best_score = final
            best_id = sid

    # 5) 输出解释
    st = st_by_id.get(best_id) or {}
    duration = st.get("duration")
    duration_text = f"{duration}分钟" if duration is not None else "适中"

    analysis = [
        f"宠物类型：{_pet_type_text(profile.petType)} → 适合{duration_text}服务",
        f"宠物年龄：{profile.petAge}岁 → 建议选择更完整的告别与纪念服务" if profile.petAge >= 8 else f"宠物年龄：{profile.petAge}岁 → 建议选择舒适且高性价比的服务组合",
        f"离世原因：{_cause_text(profile.deathCause)} → 需要更温馨的告别仪式" if _norm_cause(profile.deathCause) == "disease" else f"离世原因：{_cause_text(profile.deathCause)} → 建议更平稳的告别流程",
    ]
    if profile.participants is not None:
        analysis.append(f"参与告别人数：{int(profile.participants)}人 → 建议选择更从容的告别流程与时间安排")
    if profile.budgetMin is not None or profile.budgetMax is not None:
        bmin = int(profile.budgetMin or 0)
        bmax = "不限" if profile.budgetMax is None else str(int(profile.budgetMax))
        analysis.append(f"预算范围：{bmin} - {bmax}元 → 优先匹配预算内的套餐")

    score_pct = max(1.0, min(100.0, best_score * 100.0))
    algo = f"基于协同过滤算法，分析{similar_users_count}个相似用户的选择"
    warning = ""
    if similar_users_count == 0:
        warning = "相似用户不足，已更多依赖规则引擎"
    elif cf_confidence < 0.15:
        warning = "相似用户样本较少，已更多依赖规则引擎（预算与偏好优先）"

    return RecommendResponse(
        recommendedServiceTypeId=best_id,
        score=score_pct,
        similarUsers=similar_users_count,
        analysis=analysis,
        algorithm=f"{algo}（CF权重{int(round(cf_weight * 100))}%，规则权重{int(round(rule_weight * 100))}%）",
        warning=warning,
    )
