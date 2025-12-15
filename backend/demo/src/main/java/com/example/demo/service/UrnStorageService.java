package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.dto.UrnStorageCreateDTO;
import com.example.demo.entity.UrnStorage;
import com.example.demo.entity.Order;
import com.example.demo.repository.UrnStorageRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.vo.UrnStorageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UrnStorageService {
    @Autowired
    private UrnStorageRepository urnStorageRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserService userService;

    /**
     * 创建骨灰寄存记录
     */
    @Transactional
    public UrnStorageVO createUrnStorage(Long userId, UrnStorageCreateDTO dto) {
        // 验证订单是否存在
        Optional<Order> orderOpt = orderRepository.findById(dto.getOrderId());
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }
        
        Order order = orderOpt.get();
        // 验证订单是否属于当前用户
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        UrnStorage urnStorage = new UrnStorage();
        urnStorage.setOrderId(dto.getOrderId());
        urnStorage.setUserId(userId);
        urnStorage.setPetName(dto.getPetName());
        urnStorage.setPetType(dto.getPetType());
        urnStorage.setUrnNo(dto.getUrnNo());
        urnStorage.setStorageDate(dto.getStorageDate());
        urnStorage.setStoragePeriod(dto.getStoragePeriod());
        // 计算到期日期
        if (dto.getStorageDate() != null && dto.getStoragePeriod() != null) {
            urnStorage.setExpiryDate(dto.getStorageDate().plusMonths(dto.getStoragePeriod()));
        }
        urnStorage.setStorageLocation(dto.getStorageLocation());
        urnStorage.setRemark(dto.getRemark());
        urnStorage.setStatus(-1); // 待审批

        urnStorage = urnStorageRepository.save(urnStorage);
        return convertToVO(urnStorage);
    }

    /**
     * 获取骨灰寄存列表
     */
    public PageResult<UrnStorageVO> getUrnStorageList(Long userId, Integer status, String petName,
                                                      String urnNo, Long orderId, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<UrnStorage> page = urnStorageRepository.findByConditions(userId, status, petName, urnNo, orderId, pageable);
        
        return new PageResult<>(
            page.getContent().stream().map(this::convertToVO).collect(Collectors.toList()),
            page.getTotalElements(),
            pageSize,
            pageNum
        );
    }

    /**
     * 获取骨灰寄存详情
     */
    public UrnStorageVO getUrnStorageDetail(Long id) {
        Optional<UrnStorage> urnStorageOpt = urnStorageRepository.findById(id);
        if (urnStorageOpt.isEmpty()) {
            throw new RuntimeException("骨灰寄存记录不存在");
        }
        return convertToVO(urnStorageOpt.get());
    }

    /**
     * 更新骨灰寄存状态（取回）
     */
    @Transactional
    public UrnStorageVO retrieveUrnStorage(Long id, Long userId) {
        Optional<UrnStorage> urnStorageOpt = urnStorageRepository.findById(id);
        if (urnStorageOpt.isEmpty()) {
            throw new RuntimeException("骨灰寄存记录不存在");
        }
        
        UrnStorage urnStorage = urnStorageOpt.get();
        // 验证权限：只有寄存人或管理员可以取回
        if (!urnStorage.getUserId().equals(userId)) {
            var userInfo = userService.getUserInfo(userId);
            if (userInfo.getRole() == null || userInfo.getRole() != 2) {
                throw new RuntimeException("无权操作");
            }
        }
        
        urnStorage.setStatus(1); // 已取回
        urnStorage = urnStorageRepository.save(urnStorage);
        return convertToVO(urnStorage);
    }

    /**
     * 更新骨灰寄存信息
     */
    @Transactional
    public UrnStorageVO updateUrnStorage(Long id, UrnStorageCreateDTO dto, Long userId) {
        Optional<UrnStorage> urnStorageOpt = urnStorageRepository.findById(id);
        if (urnStorageOpt.isEmpty()) {
            throw new RuntimeException("骨灰寄存记录不存在");
        }
        
        UrnStorage urnStorage = urnStorageOpt.get();
        // 验证权限
        var userInfo = userService.getUserInfo(userId);
        if (userInfo.getRole() == null || userInfo.getRole() != 2) {
            if (!urnStorage.getUserId().equals(userId)) {
                throw new RuntimeException("无权操作");
            }
        }
        
        urnStorage.setPetName(dto.getPetName());
        urnStorage.setPetType(dto.getPetType());
        urnStorage.setUrnNo(dto.getUrnNo());
        urnStorage.setStorageDate(dto.getStorageDate());
        urnStorage.setStoragePeriod(dto.getStoragePeriod());
        if (dto.getStorageDate() != null && dto.getStoragePeriod() != null) {
            urnStorage.setExpiryDate(dto.getStorageDate().plusMonths(dto.getStoragePeriod()));
        }
        urnStorage.setStorageLocation(dto.getStorageLocation());
        urnStorage.setRemark(dto.getRemark());
        
        urnStorage = urnStorageRepository.save(urnStorage);
        return convertToVO(urnStorage);
    }

    /**
     * 删除骨灰寄存记录
     */
    @Transactional
    public boolean deleteUrnStorage(Long id, Long userId) {
        Optional<UrnStorage> urnStorageOpt = urnStorageRepository.findById(id);
        if (urnStorageOpt.isEmpty()) {
            throw new RuntimeException("骨灰寄存记录不存在");
        }
        
        UrnStorage urnStorage = urnStorageOpt.get();
        // 管理员可删除任意记录；宠主仅可撤销自己“待审批”的申请
        var userInfo = userService.getUserInfo(userId);
        Integer role = userInfo.getRole();
        if (role != null && role == 2) {
            urnStorageRepository.delete(urnStorage);
            return true;
        }

        // 宠主撤销：只能撤销自己的待审批记录
        if (role == null || role == 0) {
            if (!urnStorage.getUserId().equals(userId)) {
                throw new RuntimeException("无权删除");
            }
            if (urnStorage.getStatus() == null || urnStorage.getStatus() != -1) {
                throw new RuntimeException("仅可撤销待审批的寄存申请");
            }
            urnStorageRepository.delete(urnStorage);
            return true;
        }

        throw new RuntimeException("无权删除");
    }

    /**
     * 检查并更新到期状态
     */
    @Transactional
    public void checkAndUpdateExpiredStatus() {
        LocalDate today = LocalDate.now();
        List<UrnStorage> expiredStorages = urnStorageRepository.findExpiringSoon(
            today.minusDays(30), today
        );
        
        for (UrnStorage storage : expiredStorages) {
            if (storage.getExpiryDate().isBefore(today) || storage.getExpiryDate().isEqual(today)) {
                if (storage.getStatus() == 0) {
                    storage.setStatus(2); // 已到期
                    urnStorageRepository.save(storage);
                }
            }
        }
    }

    private UrnStorageVO convertToVO(UrnStorage urnStorage) {
        UrnStorageVO vo = new UrnStorageVO();
        BeanUtils.copyProperties(urnStorage, vo);
        
        // 设置订单信息
        try {
            Optional<Order> orderOpt = orderRepository.findById(urnStorage.getOrderId());
            if (orderOpt.isPresent()) {
                vo.setOrderNo(orderOpt.get().getOrderNo());
            }
        } catch (Exception e) {
            // 忽略
        }
        
        // 设置用户信息
        try {
            var userVO = userService.getUserInfo(urnStorage.getUserId());
            vo.setUserName(userVO.getUsername());
            vo.setUserPhone(userVO.getPhone());
        } catch (Exception e) {
            // 忽略
        }
        
        // 设置状态文本
        vo.setStatusText(getStatusText(urnStorage.getStatus()));
        
        return vo;
    }

    /**
     * 审批骨灰寄存请求
     */
    @Transactional
    public UrnStorageVO approveUrnStorageRequest(Long id, Long userId, boolean approved) {
        Optional<UrnStorage> urnStorageOpt = urnStorageRepository.findById(id);
        if (urnStorageOpt.isEmpty()) {
            throw new RuntimeException("骨灰寄存记录不存在");
        }
        
        UrnStorage urnStorage = urnStorageOpt.get();
        // 验证权限：只有管理员或服务人员可以审批
        var userInfo = userService.getUserInfo(userId);
        if (userInfo.getRole() == null || (userInfo.getRole() != 1 && userInfo.getRole() != 2)) {
            throw new RuntimeException("无权审批");
        }
        
        if (urnStorage.getStatus() != -1) {
            throw new RuntimeException("该记录不是待审批状态");
        }
        
        if (approved) {
            urnStorage.setStatus(0); // 寄存中
        } else {
            urnStorage.setStatus(3); // 已拒绝（使用3表示已拒绝）
        }
        
        urnStorage = urnStorageRepository.save(urnStorage);
        return convertToVO(urnStorage);
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case -1: return "待审批";
            case 0: return "寄存中";
            case 1: return "已取回";
            case 2: return "已到期";
            case 3: return "已拒绝";
            default: return "未知";
        }
    }
}

