package com.shiyq.convert;

import com.shiyq.entity.DO.Illustration;
import com.shiyq.entity.VO.IllustrationVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface IllustrationConvert {
    IllustrationConvert INSTANCE = Mappers.getMapper(IllustrationConvert.class);

    /**
     * 插画DOList --> 插画VOList
     */
    List<IllustrationVO> toVOList(List<Illustration> list);

    /**
     * 插画DO --> 插画VO
     */
    IllustrationVO toVO(Illustration illustration);

}
