package com.juju.beautiful.proxy.staticproxy;

import com.juju.beautiful.proxy.BuyHouseImpl;
import com.juju.beautiful.proxy.IBuyHouse;

/**
 * @author juju_liu
 */
public class StaticProxyMain {
    public static void main(String[] args) {
        IBuyHouse buyHouse = new BuyHouseImpl();
        buyHouse.buyHouse();

        BuyHouseProxy proxy = new BuyHouseProxy(new BuyHouseImpl());
        proxy.buyHouse();
    }
}
