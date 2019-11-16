package com.juju.beautiful.proxy.dynamicproxy;

import com.juju.beautiful.proxy.BuyHouseImpl;
import com.juju.beautiful.proxy.IBuyHouse;

import java.lang.reflect.Proxy;

/**
 * @author juju_liu
 */
public class DynamicProxyMain {
    public static void main(String[] args) {
        IBuyHouse buyHouse = new BuyHouseImpl();
        IBuyHouse proxy = (IBuyHouse) Proxy.newProxyInstance(IBuyHouse.class.getClassLoader()
                , new Class[]{IBuyHouse.class}, new DynamicProxyHandler(buyHouse));
        proxy.buyHouse();
    }
}
