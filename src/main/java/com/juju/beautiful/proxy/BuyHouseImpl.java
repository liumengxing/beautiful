package com.juju.beautiful.proxy;

/**
 * 服务实现类
 * @author juju_liu
 */
public class BuyHouseImpl implements IBuyHouse {
    @Override
    public void findHouse() {
        System.out.println("find house");
    }

    @Override
    public void buyHouse() {
        System.out.println("buy house");
    }

    @Override
    public void decorateHouse(String outputStr) {
        System.out.println("decorate house");
        System.out.println(outputStr);
    }
}
