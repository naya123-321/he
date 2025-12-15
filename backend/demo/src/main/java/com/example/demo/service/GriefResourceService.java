package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.dto.GriefResourceCreateDTO;
import com.example.demo.entity.GriefResource;
import com.example.demo.repository.GriefResourceRepository;
import com.example.demo.vo.GriefResourceVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GriefResourceService {
    @Autowired
    private GriefResourceRepository griefResourceRepository;

    /**
     * 宠主端：获取启用资源（可按类型筛选）
     */
    public List<GriefResourceVO> getEnabledResources(String type) {
        List<GriefResource> list;
        if (type != null && !type.isEmpty()) {
            list = griefResourceRepository.findByStatusAndType(1, type);
        } else {
            list = griefResourceRepository.findByStatus(1);
        }
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 管理端：分页查询
     */
    public PageResult<GriefResourceVO> getResourceList(String type, Integer status, Integer pageNum, Integer pageSize) {
        int p = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int s = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        PageRequest pageable = PageRequest.of(p - 1, s, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<GriefResource> page = griefResourceRepository.findByConditions(type, status, pageable);
        List<GriefResourceVO> records = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(records, page.getTotalElements(), s, p);
    }

    /**
     * 详情
     */
    public GriefResourceVO getDetail(Long id) {
        GriefResource entity = griefResourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
        return toVO(entity);
    }

    /**
     * 创建
     */
    @Transactional
    public GriefResourceVO create(GriefResourceCreateDTO dto) {
        GriefResource entity = new GriefResource();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) entity.setStatus(1);
        if (entity.getLikeCount() == null) entity.setLikeCount(0);
        if (entity.getMemberCount() == null) entity.setMemberCount(dto.getMemberCount() == null ? 0 : dto.getMemberCount());
        GriefResource saved = griefResourceRepository.save(entity);
        return toVO(saved);
    }

    /**
     * 更新
     */
    @Transactional
    public GriefResourceVO update(Long id, GriefResourceCreateDTO dto) {
        GriefResource entity = griefResourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
        BeanUtils.copyProperties(dto, entity, "id", "createTime", "likeCount");
        GriefResource saved = griefResourceRepository.save(entity);
        return toVO(saved);
    }

    /**
     * 删除
     */
    @Transactional
    public void delete(Long id) {
        if (!griefResourceRepository.existsById(id)) {
            throw new RuntimeException("资源不存在");
        }
        griefResourceRepository.deleteById(id);
    }

    /**
     * 更新状态
     */
    @Transactional
    public GriefResourceVO updateStatus(Long id, Integer status) {
        GriefResource entity = griefResourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
        entity.setStatus(status);
        GriefResource saved = griefResourceRepository.save(entity);
        return toVO(saved);
    }

    private GriefResourceVO toVO(GriefResource entity) {
        GriefResourceVO vo = new GriefResourceVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setStatusText(entity.getStatus() != null && entity.getStatus() == 1 ? "启用" : "禁用");
        vo.setTypeText(getTypeText(entity.getType()));
        return vo;
    }

    private String getTypeText(String type) {
        if (type == null) return "未知";
        switch (type) {
            case "article":
                return "文章";
            case "video":
                return "视频";
            default:
                return "未知";
        }
    }
}



