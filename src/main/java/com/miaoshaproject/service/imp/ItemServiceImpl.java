package com.miaoshaproject.service.imp;

import com.miaoshaproject.dao.ItemDoMapper;
import com.miaoshaproject.dao.ItemStockDoMapper;
import com.miaoshaproject.dataobject.ItemStockDo;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.PromoModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.miaoshaproject.dataobject.ItemDo;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemDoMapper itemDOMapper;

    @Autowired
    private ItemStockDoMapper itemStockDOMapper;

    @Autowired
    private PromoService promoService;



    @Override
    @Transactional
    public ItemModel creatItem(ItemModel itemModel) throws BusinessException {
        //校验入参
        ValidationResult result = validator.validationResult(itemModel);
        if(result.isHasError()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR, result.getErrMsg());
        }

        //转化ItemModel -> DataObject
        ItemDo itemDO = this.convertItemDOFromItemModel(itemModel);

        //写入数据库
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());

        ItemStockDo itemStockDO = this.convertItemStockDOFromItemModel(itemModel);

        itemStockDOMapper.insertSelective(itemStockDO);

        //返回创建完成的对象
        return this.getItemById(itemModel.getId());
    }
    private ItemDo convertItemDOFromItemModel(ItemModel itemModel) {
        if(itemModel == null) {
            return null;
        }
        ItemDo itemDO = new ItemDo();
        BeanUtils.copyProperties(itemModel, itemDO);
        //这里price由BigDecimal转为double
        itemDO.setPrice(itemModel.getPrice().doubleValue());
        return itemDO;
    }

    private ItemStockDo convertItemStockDOFromItemModel(ItemModel itemModel) {
        if(itemModel == null) {
            return null;
        }
        ItemStockDo itemStockDO = new ItemStockDo();
        BeanUtils.copyProperties(itemModel, itemStockDO);
        //这里price由BigDecimal转为double
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());
        return itemStockDO;
    }



    @Override
    public List<ItemModel> listItem() {
        List<ItemDo> itemDoList=itemDOMapper.listItem();
        List<ItemModel> itemModelList=itemDoList.stream().map(itemDo -> {
            ItemStockDo itemStockDo=itemStockDOMapper.selectByItemId(itemDo.getId());
           ItemModel itemModel=this.convertModelFromDO(itemDo,itemStockDo);
           return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    public ItemModel getItemById(Integer id) {
        ItemDo itemDO = itemDOMapper.selectByPrimaryKey(id);
        if(itemDO == null) {
            return null;
        }
        //获取库存数量
        ItemStockDo itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

        //将DO -> Model
        ItemModel itemModel = convertModelFromDO(itemDO, itemStockDO);

        //获取秒杀商品活动信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if(promoModel != null && promoModel.getStatus().intValue() != 3) {
            itemModel.setPromoModel(promoModel);
        }

        return itemModel;
    }

    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount); //mybatis会返回受影响而更新的条目数，更新失败（不满足条件），则返回0
        if (affectedRow>0){
            //更新库存成功
            return true;
        }else {
            //更新库存失败
            return false;
        }
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemDOMapper.increaseSales(itemId,amount);
    }

    private ItemModel convertModelFromDO(ItemDo itemDO, ItemStockDo itemStockDO) {
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
        itemModel.setStock(itemStockDO.getStock());

        return itemModel;
    }
}
