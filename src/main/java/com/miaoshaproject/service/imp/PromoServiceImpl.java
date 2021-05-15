package com.miaoshaproject.service.imp;

import com.miaoshaproject.dao.PromoDOMapper;
import com.miaoshaproject.dataobject.PromoDO;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.PromoModel;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的活动信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        PromoModel promoModel = convertFromDataObject(promoDO);

        if (promoModel == null) {
            return null;
        }
        //判断当前时间活动情况，将当前活动情况写入

        System.out.println(promoModel.getStartDate().isAfterNow());
        System.out.println(promoModel.getStartDate());
        System.out.println(DateTimeUtils.currentTimeMillis());

        if (promoModel.getStartDate().isAfterNow()) {
            promoModel.setStatus(1);    //未开始
        } else if (promoModel.getEndDate().isBeforeNow()) {
            promoModel.setStatus(3);    //已结束
        } else {
            promoModel.setStatus(2);    //进行中
        }
        return promoModel;
    }

    private PromoModel convertFromDataObject(PromoDO promoDO) {
        if (promoDO == null) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}
