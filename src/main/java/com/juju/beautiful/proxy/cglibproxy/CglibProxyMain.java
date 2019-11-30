package com.juju.beautiful.proxy.cglibproxy;

import com.juju.beautiful.proxy.BuyHouseImpl;
import com.juju.beautiful.proxy.IBuyHouse;

/**
 * @author juju_liu
 */
public class CglibProxyMain {
    public static void main(String[] args) {
        IBuyHouse buyHouse = new BuyHouseImpl();
        CglibProxy cglibProxy = new CglibProxy();
        BuyHouseImpl buyHouseCglibProxy = (BuyHouseImpl) cglibProxy.getInstance(buyHouse);
        buyHouseCglibProxy.decorateHouse("juju");
    }
}
