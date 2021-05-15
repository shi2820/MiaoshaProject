package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobject.ItemVo;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller("Item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*", originPatterns = "*") //DEFAULT_ALLOWED_HEADERS:允许跨域传输所有的header参数，将用于使用token放入header域做session共享的跨域请求
public class ItemController extends BaseController{

    @Autowired
    private ItemService itemService;


    //创建商品的Controller
    @RequestMapping(value = "/create", method = {RequestMethod.POST,RequestMethod.GET}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name = "title") String title,
                                       @RequestParam(name = "description") String description,
                                       @RequestParam(name = "price") BigDecimal price,
                                       @RequestParam(name = "stock") Integer stock,
                                       @RequestParam(name = "imgUrl") String imgUrl) throws BusinessException {
        //封装service请求用来创建商品
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescriptionTitle(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn = itemService.creatItem(itemModel);
        ItemVo itemVO = convertVOFromModel(itemModelForReturn);
        return CommonReturnType.creat(itemVO);
    }

    private ItemVo convertVOFromModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemVo itemVO = new ItemVo();
        BeanUtils.copyProperties(itemModel, itemVO);
       // itemVO.setPrice(itemModel.getPrice().doubleValue());

        //判断有活动情况;0表示活动未开始
        if (itemModel.getPromoModel() != null) {
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }

    //商品详情页浏览
    @RequestMapping(value = "/get", method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id")Integer id){
        ItemModel itemModel=itemService.getItemById(id);
        ItemVo itemVo=convertVOFromModel(itemModel);
        return CommonReturnType.creat(itemVo);
    }

    //商品列表页面浏览
    @RequestMapping(value = "/list", method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listItem(){
        List<ItemModel> itemModelList=itemService.listItem();

        //使用stream api将list内的itemModel转化为itemVo
        List<ItemVo> itemVoList=itemModelList.stream().map(itemModel -> {
            ItemVo itemVo=this.convertVOFromModel(itemModel);
            return itemVo;
        }).collect(Collectors.toList());
        return CommonReturnType.creat(itemVoList);
    }

}
