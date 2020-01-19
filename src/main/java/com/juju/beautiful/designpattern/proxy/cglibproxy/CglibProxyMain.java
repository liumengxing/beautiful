package com.juju.beautiful.designpattern.proxy.cglibproxy;

import com.juju.beautiful.designpattern.proxy.BuyHouseImpl;
import com.juju.beautiful.designpattern.proxy.IBuyHouse;

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
