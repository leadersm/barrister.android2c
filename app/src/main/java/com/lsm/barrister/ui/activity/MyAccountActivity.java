package com.lsm.barrister.ui.activity;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.androidquery.AQuery;
import com.lsm.barrister.R;
import com.lsm.barrister.app.UserHelper;
import com.lsm.barrister.data.entity.Account;
import com.lsm.barrister.data.entity.IncomeDetailItem;
import com.lsm.barrister.data.io.Action;
import com.lsm.barrister.data.io.IO;
import com.lsm.barrister.data.io.app.GetIncomeDetailListReq;
import com.lsm.barrister.data.io.app.GetMyAccountReq;
import com.lsm.barrister.data.io.app.GetStudyListReq;
import com.lsm.barrister.ui.UIHelper;
import com.lsm.barrister.ui.adapter.IncomeListAdapter;
import com.lsm.barrister.ui.adapter.LoadMoreListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * 我的账户页
 * 1.余额
 * 2.银行卡信息
 * 3.提现记录
 * 4.入账记录
 */
public class MyAccountActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener,UserHelper.OnAccountUpdateListener{

    private static final String TAG = MyAccountActivity.class.getSimpleName();

    GetMyAccountReq mGetAccountReq;

    GetIncomeDetailListReq mGetIncomeListReq;
    SwipeRefreshLayout mSwipeRefreshLayout;

    AQuery aq;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);
        setupToolbar();
        aq = new AQuery(this);

        aq.id(R.id.btn_myaccount_bankcard).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Account account = UserHelper.getInstance().getAccount();

                if(account==null){
                    UIHelper.showToast(getApplicationContext(),"获取账户信息失败.");
                    Log.d(TAG,"load account failed");
                    return;
                }

                boolean bind = account.getBankCardBindStatus().equals(Account.CARD_STATUS_BOUND);

                //TODO 银行卡
                UIHelper.goBankcardActivity(MyAccountActivity.this,bind?bankcard:null);//result.account.getBankCard());
            }
        });

        aq.id(R.id.btn_myaccount_extract).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO 提现
                UIHelper.goGetMoneyActivity(MyAccountActivity.this);
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_green_light, android.R.color.holo_orange_light,
                android.R.color.holo_red_light, android.R.color.holo_blue_light);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_income_detail);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new IncomeListAdapter(items, new LoadMoreListener() {
            @Override
            public void onLoadMore() {
                loadMore();
            }

            @Override
            public boolean hasMore() {
                return mGetIncomeListReq != null && page + 1 <= mGetIncomeListReq.getTotalPage(total, GetStudyListReq.pageSize);
            }
        });

        mRecyclerView.setAdapter(mAdapter);

        loadAccount();

        loadIncomeList();

        UserHelper.getInstance().addOnAccountUpdateListener(this);

    }

    private void loadAccount() {
        mGetAccountReq = new GetMyAccountReq(this);
        mGetAccountReq.execute(new Action.Callback<IO.GetAccountResult>() {
            @Override
            public void progress() {

            }

            @Override
            public void onError(int errorCode, String msg) {
                UIHelper.showToast(getApplicationContext(),msg);
            }

            @Override
            public void onCompleted(IO.GetAccountResult result) {
                UserHelper.getInstance().setAccount(result.account);
                UserHelper.getInstance().updateAccount();
            }
        });
    }

    int total;

    private void loadIncomeList() {
        mGetIncomeListReq  = new GetIncomeDetailListReq(this,page = 1);
        mGetIncomeListReq.execute(new Action.Callback<IO.GetIncomeDetailListResult>() {
            @Override
            public void progress() {

            }

            @Override
            public void onError(int errorCode, String msg) {

                mSwipeRefreshLayout.setRefreshing(false);
                UIHelper.showToast(getApplicationContext(),msg);

            }

            @Override
            public void onCompleted(IO.GetIncomeDetailListResult result) {

                mSwipeRefreshLayout.setRefreshing(false);
                total = result.total;

                if(result.incomeDetails!=null){
                    items.clear();
                    items.addAll(result.incomeDetails);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    int page = 1;



    RecyclerView mRecyclerView;
    LinearLayoutManager mLayoutManager;
    IncomeListAdapter mAdapter;

    List<IncomeDetailItem> items = new ArrayList<>();

    Account.BankCard bankcard;

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_my_account);
    }

    boolean isLoadingMore = false;
    private void loadMore(){

        if(isLoadingMore)
            return;

        new GetIncomeDetailListReq(this,++page).execute(new Action.Callback<IO.GetIncomeDetailListResult>() {

            @Override
            public void progress() {
                isLoadingMore = true;
            }

            @Override
            public void onError(int errorCode, String msg) {
                isLoadingMore = false;

                --page;
                UIHelper.showToast(getApplicationContext(),msg);
            }

            @Override
            public void onCompleted(IO.GetIncomeDetailListResult result) {
                isLoadingMore = false;

                if(result!=null && result.incomeDetails!=null){
                    items.addAll(result.incomeDetails);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onRefresh() {
        loadIncomeList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserHelper.getInstance().removeAccountListener(this);
    }

    @Override
    public void onUpdateAccount(Account account) {

        aq.id(R.id.tv_myaccount_balance).text(String.format(Locale.CHINA,"%.2f元",account.getRemainingBalance()));
        aq.id(R.id.tv_myaccount_total).text(String.format(Locale.CHINA,"%.2f元",account.getTotalIncome()));

        bankcard = account.getBankCard();

    }

}
