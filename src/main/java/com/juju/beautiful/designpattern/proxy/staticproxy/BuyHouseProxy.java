package com.juju.beautiful.designpattern.proxy.staticproxy;

import com.juju.beautiful.designpattern.proxy.IBuyHouse;

/**
 * 代理类
 *
 * @author juju_liu
 */
public class BuyHouseProxy implements IBuyHouse {
    private IBuyHouse buyHouse;

    public BuyHouseProxy(final IBuyHouse buyHouse) {
        this.buyHouse = buyHouse;
    }

    @Override
    public void findHouse() {
        buyHouse.findHouse();
    }

    @Override
    public void buyHouse() {
        buyHouse.buyHouse();
    }

    @Override
    public void decorateHouse(String outputStr) {
        buyHouse.decorateHouse(outputStr);
    }
}
