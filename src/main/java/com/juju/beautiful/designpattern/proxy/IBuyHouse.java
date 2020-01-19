package com.juju.beautiful.designpattern.proxy;

/**
 * 服务类接口
 *
 * @author juju_liu
 */
public interface IBuyHouse {
    /**
     * findHouse
     */
    void findHouse();

    /**
     * buyHouse
     */
    void buyHouse();

    /**
     * decorateHouse
     */
    void decorateHouse(String outputStr);
}
