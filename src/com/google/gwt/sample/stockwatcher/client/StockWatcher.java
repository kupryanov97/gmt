package com.google.gwt.sample.stockwatcher.client;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Random;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.event.dom.client.KeyDownHandler;
import java.util.ArrayList;
import com.google.gwt.user.client.Timer;
import com.google.gwt.i18n.client.DateTimeFormat;
import java.util.Date;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.i18n.client.NumberFormat;
import java.io.IOException;
import java.io.*;
public class StockWatcher implements EntryPoint {

  private VerticalPanel mainPanel = new VerticalPanel();
  private FlexTable stocksFlexTable = new FlexTable();
  private HorizontalPanel addPanel = new HorizontalPanel();
  private TextBox newSymbolTextBox = new TextBox();
  private Button addStockButton = new Button("Добавить");
  private Label lastUpdatedLabel = new Label();
  private ArrayList<String> stocks = new ArrayList<String>();
  private static final int REFRESH_INTERVAL = 1000; // ms
  private StockPriceServiceAsync stockPriceSvc = GWT.create(StockPriceService.class);
  private Label errorMsgLabel = new Label();
  /**
   * Entry point method.
   */
  public void onModuleLoad() {
    // создаём таблицу и даём ей стили
    stocksFlexTable.setText(0, 0, "Вы ввели");
    stocksFlexTable.setText(0, 1, "Стоимость");
    stocksFlexTable.setText(0, 2, "Change");
    stocksFlexTable.setText(0, 3, "Что-то");
    stocksFlexTable.setText(0, 4, "Удалить");
    stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
    stocksFlexTable.addStyleName("watchList");
    stocksFlexTable.setCellPadding(12);
    stocksFlexTable.getCellFormatter().addStyleName(0, 1, "watchListNumericColumn");
    stocksFlexTable.getCellFormatter().addStyleName(0, 2, "watchListNumericColumn");
    stocksFlexTable.getCellFormatter().addStyleName(0, 3, "watchListRemoveColumn");

// Assemble Add Stock panel.
    addPanel.add(newSymbolTextBox);
    addPanel.add(addStockButton);
    addPanel.addStyleName("addPanel");

// Assemble Main panel.
    errorMsgLabel.setStyleName("errorMessage");
    errorMsgLabel.setVisible(false);

    mainPanel.add(errorMsgLabel);
    mainPanel.add(stocksFlexTable);
    mainPanel.add(addPanel);
    mainPanel.add(lastUpdatedLabel);

    // и последнее что надо
    RootPanel.get("stockList").add(mainPanel);

    // курсор в вводе по дефолту
    newSymbolTextBox.setFocus(true);

    // автообновление
    Timer refreshTimer = new Timer() {
      @Override
      public void run() {
        refreshWatchList();
      }
    };
    refreshTimer.scheduleRepeating(REFRESH_INTERVAL);

    // Listen for mouse events on the Add button.
    addStockButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        addStock();
      }
    });

    // Listen for keyboard events in the input box.
    newSymbolTextBox.addKeyDownHandler(new KeyDownHandler() {
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          addStock();
        }
      }
    });
  }
  /**
   * Add stock to FlexTable. Executed when the user clicks the addStockButton or
   * presses enter in the newSymbolTextBox.
   */
  //Заполняем таблицуZ
  private void refreshWatchList() {
    // Initialize the service proxy.
    if (stockPriceSvc == null) {
      stockPriceSvc = GWT.create(StockPriceService.class);
    }
    AsyncCallback<StockPrices[]> callback = new AsyncCallback<StockPrices[]>() {
      public void onFailure(Throwable caught) {
        // If the stock code is in the list of delisted codes, display an error message.
        String details = caught.getMessage();
        if (caught instanceof DelistedException) {
          details = "Company '" + ((DelistedException) caught).getSymbol() + "' was delisted";
        }

        errorMsgLabel.setText("Error: " + details);
        errorMsgLabel.setVisible(true);
      }

      public void onSuccess(StockPrices[] result) {
        updateTable(result);
      }
    };

    // Make the call to the stock price service.
    //stockPriceSvc.getPrices(stocks.toArray(new String[0]), callback);
  }
  //обновляем данные
  private void updateTable(StockPrices[] prices) {
    for (int i=0; i < prices.length; i++) {
      updateTable(prices[i]);
    }

    // Display timestamp showing last refresh.
    lastUpdatedLabel.setText("Last update : " +
            DateTimeFormat.getMediumDateTimeFormat().format(new Date()));

    // Clear any errors.
    errorMsgLabel.setVisible(false);
  }
  private void updateTable(StockPrices price) {
    // Make sure the stock is still in the stock table.
    if (!stocks.contains(price.getSymbol())) {
      return;
    }
    int row = stocks.indexOf(price.getSymbol()) + 1;

    // Format the data in the Price and Change fields.
    String priceText = NumberFormat.getFormat("#,##0.00").format(
            price.getPrice());
    NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.0;-#,##0.0");
    String changeText = changeFormat.format(price.getChange());
    String changePercentText = changeFormat.format(price.getChangePercent());

    // Populate the Price and Change fields with new data.
    stocksFlexTable.setText(row, 1, priceText);
    Label changeWidget = (Label)stocksFlexTable.getWidget(row, 2);
    changeWidget.setText(changeText + " (" + changePercentText + "%)");

// Change the color of text in the Change field based on its value.
    String changeStyleName = "noChange";
    if (price.getChangePercent() < -0.1f) {
      changeStyleName = "negativeChange";
    }
    else if (price.getChangePercent() > 0.1f) {
      changeStyleName = "positiveChange";
    }
    changeWidget.setStyleName(changeStyleName);}
  private void addStock() {
    final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
    newSymbolTextBox.setFocus(true);
    newSymbolTextBox.setText("");

    if (stocks.contains(symbol))
      return;
    int row = stocksFlexTable.getRowCount();
    stocks.add(symbol);
    stocksFlexTable.setText(row, 0, symbol);
    stocksFlexTable.setWidget(row, 2, new Label());
    stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
    stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
    stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");
    // Add a button to remove a stock from the table.
    Button removeStockButton = new Button("REM");
    removeStockButton.addStyleDependentName("remove");
    Button removeStockButton1 = new Button("Кекнуть");
    removeStockButton.addStyleDependentName("remove");
    removeStockButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        int removedIndex = stocks.indexOf(symbol);
        stocks.remove(removedIndex);
        stocksFlexTable.removeRow(removedIndex + 1);
        Window.alert("Удалено");
      }
    });
    removeStockButton1.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Window.alert("KEEEEEK");
      }
    });
    stocksFlexTable.setWidget(row, 4, removeStockButton);
    stocksFlexTable.setWidget(row, 3, removeStockButton1);

    // Get the stock price.
    refreshWatchList();
  }

}