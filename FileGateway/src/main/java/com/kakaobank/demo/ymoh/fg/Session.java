package com.kakaobank.demo.ymoh.fg;

import com.kakaobank.demo.ymoh.Server;

public interface Session extends Server {

    String getId();

    void addOperator(SessionOperator operator);

    void addListener(SessionListener listener);

}
