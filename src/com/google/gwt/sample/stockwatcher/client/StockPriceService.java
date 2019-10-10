package com.google.gwt.sample.stockwatcher.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("stockPrice")
public interface StockPriceService extends RemoteService {

    StockPrices[] getPrices(String[] symbols) throws DelistedException;
}