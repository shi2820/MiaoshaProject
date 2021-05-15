package com.miaoshaproject.service.imp;

import com.miaoshaproject.dao.OrderDoMapper;
import com.miaoshaproject.dao.SequenceDOMapper;
import com.miaoshaproject.dataobject.OrderDo;
import com.miaoshaproject.dataobject.SequenceDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDoMapper orderDoMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId,Integer amount) throws BusinessException {


        //1.校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确
        ItemModel itemModel = itemService.getItemById(itemId);
        if (itemModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR,"商品信息不存在");
        }

        UserModel userModel = userService.getUserById(userId);
        if (userModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR,"用户信息不存在");
        }

        if (amount<=0||amount>99){
            throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR,"数量信息不正确");
        }

        //校验活动信息
        if (promoId != null) {
            //检验对应活动是否存在这个适用商品
            if (promoId.intValue() != itemModel.getPromoModel().getId()) {
                throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR, "活动信息不正确");
            } else if (itemModel.getPromoModel().getStatus() != 2){
                //校验活动是否在进行中
                throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR, "活动尚未开始");
            }
        }


        //2.落单减库存
        boolean result=itemService.decreaseStock(itemId,amount);
        if (!result){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        //3.订单入库
        OrderModel orderModel=new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        //判断为原价还是活动价格
        if (promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));

        //生成交易流水号，订单号
        orderModel.setId(generateOrderNum());
        OrderDo orderDo=convertFromOrderModel(orderModel);
        orderDoMapper.insertSelective(orderDo);

        //加上商品销量
        itemService.increaseSales(itemId,amount);


        //4.返回前端
        return orderModel;
    }

    private OrderDo convertFromOrderModel(OrderModel orderModel){
        if (orderModel==null){
            return null;
        }

        OrderDo orderDo=new OrderDo();
        BeanUtils.copyProperties(orderModel,orderDo);
        orderDo.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDo.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDo;
    }


    //返回订单编号
    @Transactional(propagation = Propagation.REQUIRES_NEW)  //这个注解作用是重启一个事务，运用该方法的另一个事务无论失败与否，都不影响该事务的执行情况
    String generateOrderNum(){
        //订单号有16位，
        StringBuilder stringBuilder=new StringBuilder();
        // 前8位为时间信息，年月日，（方便订单归类）
        LocalDateTime now=LocalDateTime.now();
        String nowDate=now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        stringBuilder.append(nowDate);
        // 中间6位为自增序列 （保证同一天时间内订单号不重复）
        //获取当前sequence
        int sequence=0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue()+sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr=String.valueOf(sequence);
        for (int i = 0; i <6-sequenceStr.length(); i++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        // 最后2位为分库分表位,暂时写死
        stringBuilder.append("00");
        return stringBuilder.toString();
    }
}
