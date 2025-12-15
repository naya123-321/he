package com.example.demo.service;

import com.example.demo.entity.ServiceType;
import com.example.demo.repository.ServiceTypeRepository;
import com.example.demo.vo.ServiceTypeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ServiceTypeService {
    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    /**
     * 获取所有服务类型（包括禁用的）
     */
    public List<ServiceTypeVO> getAllServiceTypes() {
        List<ServiceType> serviceTypes = serviceTypeRepository.findAll();
        return serviceTypes.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取启用的服务类型
     */
    public List<ServiceTypeVO> getEnabledServiceTypes() {
        List<ServiceType> serviceTypes = serviceTypeRepository.findByStatus(1);
        return serviceTypes.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取服务类型
     */
    public ServiceTypeVO getServiceTypeById(Long id) {
        Optional<ServiceType> serviceTypeOpt = serviceTypeRepository.findById(id);
        if (serviceTypeOpt.isEmpty()) {
            throw new RuntimeException("服务类型不存在");
        }
        return convertToVO(serviceTypeOpt.get());
    }

    /**
     * 创建服务类型
     */
    @Transactional
    public ServiceTypeVO createServiceType(ServiceTypeVO vo) {
        ServiceType serviceType = new ServiceType();
        serviceType.setName(vo.getName());
        serviceType.setDescription(vo.getDescription());
        serviceType.setPrice(vo.getPrice());
        serviceType.setDuration(vo.getDuration());
        serviceType.setProcess(vo.getProcess());
        serviceType.setStatus(vo.getStatus() != null ? vo.getStatus() : 1);
        serviceType.setIsRecommended(vo.getIsRecommended() != null ? vo.getIsRecommended() : false);
        
        ServiceType saved = serviceTypeRepository.save(serviceType);
        return convertToVO(saved);
    }

    /**
     * 更新服务类型
     */
    @Transactional
    public ServiceTypeVO updateServiceType(Long id, ServiceTypeVO vo) {
        Optional<ServiceType> serviceTypeOpt = serviceTypeRepository.findById(id);
        if (serviceTypeOpt.isEmpty()) {
            throw new RuntimeException("服务类型不存在");
        }
        
        ServiceType serviceType = serviceTypeOpt.get();
        serviceType.setName(vo.getName());
        serviceType.setDescription(vo.getDescription());
        serviceType.setPrice(vo.getPrice());
        serviceType.setDuration(vo.getDuration());
        serviceType.setProcess(vo.getProcess());
        if (vo.getStatus() != null) {
            serviceType.setStatus(vo.getStatus());
        }
        if (vo.getIsRecommended() != null) {
            serviceType.setIsRecommended(vo.getIsRecommended());
        }
        
        ServiceType saved = serviceTypeRepository.save(serviceType);
        return convertToVO(saved);
    }

    /**
     * 删除服务类型
     */
    @Transactional
    public boolean deleteServiceType(Long id) {
        Optional<ServiceType> serviceTypeOpt = serviceTypeRepository.findById(id);
        if (serviceTypeOpt.isEmpty()) {
            throw new RuntimeException("服务类型不存在");
        }
        serviceTypeRepository.deleteById(id);
        return true;
    }

    /**
     * 启用/禁用服务类型
     */
    @Transactional
    public ServiceTypeVO updateStatus(Long id, Integer status) {
        Optional<ServiceType> serviceTypeOpt = serviceTypeRepository.findById(id);
        if (serviceTypeOpt.isEmpty()) {
            throw new RuntimeException("服务类型不存在");
        }
        
        ServiceType serviceType = serviceTypeOpt.get();
        serviceType.setStatus(status);
        
        ServiceType saved = serviceTypeRepository.save(serviceType);
        return convertToVO(saved);
    }

    private ServiceTypeVO convertToVO(ServiceType serviceType) {
        ServiceTypeVO vo = new ServiceTypeVO();
        BeanUtils.copyProperties(serviceType, vo);
        return vo;
    }
}


