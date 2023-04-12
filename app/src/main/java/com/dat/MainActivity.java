package com.dat;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private ArrayAdapter adapter;
    private Button buttonC;
    private Button buttonE;
    private TextView tvFrom;
    private TextView tvTo;
    private Button buttonSync;
    private Button buttonBS;

    private void showChooseItemDialog(Button button) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn đơn vị tiền tệ");
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_choose_item, null);
        ListView listView = dialogView.findViewById(R.id.listViewItems);
        EditText edtSearch = dialogView.findViewById(R.id.editTextSearch);
        listView.setAdapter(this.adapter);
        builder.setView(dialogView);
        builder.setNegativeButton("Trở về", (dialog, which) -> {
            dialog.dismiss(); // Ẩn AlertDialog khi nhấn nút Cancel
        });
        AlertDialog alertDialog = builder.create();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String c = ((String) parent.getItemAtPosition(position)).split(" - ")[0];
            button.setText(c);
            loadRate();
            alertDialog.dismiss();
        });
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                adapter.getFilter().filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        alertDialog.show();

    }

    private Button btnFrom;
    private Button btnTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initView();
        initEvent();
        new LoadSpinner().loadSpinner();
        initButtonNumber();
    }

    private class LoadSpinner {
        private Disposable disposable;

        public void loadSpinner() {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
            disposable = Observable.fromCallable(() -> {
                        URL url = new URL("https://all.fxexchangerate.com/rss.xml");
                        Log.d("", url.toURI().toString());
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        org.w3c.dom.Document doc = db.parse(url.openStream());
                        doc.getDocumentElement().normalize();
                        NodeList nodeList = doc.getElementsByTagName("item");
                        HashMap<String, String> listCurrency = new HashMap<>();
                        for (int i = 0; i < nodeList.getLength(); i++) {
                            Node node = nodeList.item(i);
                            org.w3c.dom.Element fstElmnt = (org.w3c.dom.Element) node;
                            NodeList nameList = fstElmnt.getElementsByTagName("title");
                            org.w3c.dom.Element nameElement = (org.w3c.dom.Element) nameList.item(0);
                            String[] data = nameElement.getChildNodes().item(0).getNodeValue().split("/");
                            if (data.length > 1) {
                                String text = data[1];
                                listCurrency.put(text.substring(text.length() - 4, text.length() - 1), text.substring(0, text.length() - 6));
                            }
                        }
                        return listCurrency;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(listCurrency -> {
                        progressBar.setVisibility(View.GONE);
                        progressBar.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_out));
                        ArrayList<String> list = new ArrayList<>();
                        for (String k : listCurrency.keySet()) {
                            list.add(k + " - " + listCurrency.get(k));
                        }
                        adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, list);
                        loadRate();
                    }, throwable -> {
                        throwable.printStackTrace();
                    });
        }

        public void cancel() {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
        }
    }

    private void initView() {
        progressBar = findViewById(R.id.progressBar);
        tvFrom = findViewById(R.id.tvFrom);
        tvTo = findViewById(R.id.tvTo);
        btnFrom = findViewById(R.id.btnFrom);
        btnFrom.setOnClickListener(view -> showChooseItemDialog(btnFrom));
        btnTo = findViewById(R.id.btnTo);
        btnTo.setOnClickListener(view -> showChooseItemDialog(btnTo));
        tvFrom.setText("0");
        tvTo.setText("0");
        btnFrom.setText("USD");
        btnTo.setText("VND");
        buttonC = findViewById(R.id.buttonC);
        buttonE = findViewById(R.id.buttonE);
        buttonSync = findViewById(R.id.buttonSync);
        buttonBS = findViewById(R.id.buttonBS);
    }

    private void btnNumberClick(View view) {
        String buttonText = ((Button) view).getText().toString();
        String t = tvFrom.getText().toString();
        if (t.equals("0")) tvFrom.setText(buttonText);
        else tvFrom.setText(t + buttonText);
        String f=t+buttonText;
        if (f.contains(",")&&trainInput(f).compareTo(new BigDecimal("0"))==0) return;
        if (!rateExchange()) {
            tvFrom.setText(t);
        }
    }

    private void initEvent() {
        buttonC.setOnClickListener(view -> {
            tvFrom.setText("0");
            tvTo.setText("0");
        });
        buttonSync.setOnClickListener(view -> {
            String t = btnTo.getText().toString();
            btnTo.setText(btnFrom.getText().toString());
            btnFrom.setText(t);
            loadRate();
        });
        buttonE.setOnClickListener(view -> rateExchange());
        buttonBS.setOnClickListener(view -> {
            String tvf = tvFrom.getText().toString();
            if (tvf.equals("0")) ;
            else if (tvf.length() <= 1) tvFrom.setText("0");
            else tvFrom.setText(tvf.substring(0, tvf.length() - 1));
            rateExchange();
        });
    }

    private void initButtonNumber() {
        Button button0 = findViewById(R.id.button0);
        Button button1 = findViewById(R.id.button1);
        Button button2 = findViewById(R.id.button2);
        Button button3 = findViewById(R.id.button3);
        Button button4 = findViewById(R.id.button4);
        Button button5 = findViewById(R.id.button5);
        Button button6 = findViewById(R.id.button6);
        Button button7 = findViewById(R.id.button7);
        Button button8 = findViewById(R.id.button8);
        Button button9 = findViewById(R.id.button9);
        Button buttonP = findViewById(R.id.buttonP);
        button0.setOnClickListener(view -> btnNumberClick(view));
        button1.setOnClickListener(view -> btnNumberClick(view));
        button2.setOnClickListener(view -> btnNumberClick(view));
        button3.setOnClickListener(view -> btnNumberClick(view));
        button4.setOnClickListener(view -> btnNumberClick(view));
        button5.setOnClickListener(view -> btnNumberClick(view));
        button6.setOnClickListener(view -> btnNumberClick(view));
        button7.setOnClickListener(view -> btnNumberClick(view));
        button8.setOnClickListener(view -> btnNumberClick(view));
        button9.setOnClickListener(view -> btnNumberClick(view));
        buttonP.setOnClickListener(view -> {
            if (tvFrom.getText().toString().contains(","))
                Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            else {
                String buttonText = ((Button) view).getText().toString();
                tvFrom.setText(tvFrom.getText().toString() + buttonText);
            }
        });
    }

    private boolean rateExchange() {
        BigDecimal value = trainInput(tvFrom.getText().toString());
        String rs = formatCurrency(rate.multiply(value));
        if (rs.length() > 25) {
            Toast.makeText(this, "Số tiền quá lớn", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            tvFrom.setText(this.formatCurrency(value));
            tvTo.setText(rs);
        }
        return true;
    }

    private BigDecimal rate = new BigDecimal("1");

    private void loadRate() {
        String value = "10000000000";
        String idFrom = btnFrom.getText().toString();
        String idTo = btnTo.getText().toString();
        Observable.fromCallable(() -> {
                    if (idFrom.equals(idTo)) return new BigDecimal("1");
                    Document document = null;
                    try {
                        document = Jsoup.connect("https://" + idFrom.toLowerCase() + ".fxexchangerate.com/" + idTo.toLowerCase() + "/" + value + "-currency-rates.html").get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    BigDecimal rate=new BigDecimal(document.getElementById("srate").text());
                    return rate.divide(new BigDecimal(value));
                })
                .subscribeOn(Schedulers.io()) // Thực hiện công việc trong background thread
                .observeOn(AndroidSchedulers.mainThread()) // Kết quả trả về trên main thread
                .subscribe(result -> {
                    this.rate=result;
                    System.out.println(rate);
                    rateExchange();
                }, throwable -> {
                    throwable.printStackTrace();
                });
    }

    private String formatCurrency(BigDecimal value) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.##########");
        String formattedAmount = decimalFormat.format(value);
        return formattedAmount;
    }


    private BigDecimal trainInput(String value) {
        try {
            value = value.replace(".", "");
            value = value.replace(",", ".");
            return new BigDecimal(value);
        } catch (Exception e) {
        }
        return new BigDecimal("0");
    }
}
