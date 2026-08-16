package com.shiyq.convert;

import com.shiyq.entity.DO.Illustration;
import com.shiyq.entity.VO.IllustrationVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface IllustrationConvert {
    IllustrationConvert INSTANCE = Mappers.getMapper(IllustrationConvert.class);

    /**
     * 插画DO --> 插画VO
     */
    @Mapping(target = "url", ignore = true)
    IllustrationVO toVO(Illustration illustration);

}
