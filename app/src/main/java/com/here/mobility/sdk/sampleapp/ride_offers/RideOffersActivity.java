package com.here.mobility.sdk.sampleapp.ride_offers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.here.mobility.sdk.core.net.ResponseException;
import com.here.mobility.sdk.core.net.ResponseFuture;
import com.here.mobility.sdk.core.net.ResponseListener;
import com.here.mobility.sdk.demand.CreateRideRequest;
import com.here.mobility.sdk.demand.DemandClient;
import com.here.mobility.sdk.demand.PassengerDetails;
import com.here.mobility.sdk.demand.PublicTransportRideOffer;
import com.here.mobility.sdk.demand.Ride;
import com.here.mobility.sdk.demand.RideOffer;
import com.here.mobility.sdk.demand.RideOffersRequest;
import com.here.mobility.sdk.demand.RidePreferences;
import com.here.mobility.sdk.demand.TaxiRideOffer;
import com.here.mobility.sdk.sampleapp.R;
import com.here.mobility.sdk.sampleapp.public_transport.PublicTransportActivity;
import com.here.mobility.sdk.sampleapp.ride_status.RideStatusActivity;


import java.util.ArrayList;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class RideOffersActivity extends AppCompatActivity implements RideOffersAdapter.RideOffersListener {


    /**
     * Taxi Ride offers list Intent.extra key.
     */
    private static final String EXTRA_TAXI_RIDE_OFFER_LIST = "TAXI_RIDE_OFFER_LIST";


    /**
     * Public transportation Ride offers list Intent.extra key.
     */
    private static final String EXTRA_PT_RIDE_OFFER_LIST = "PT_RIDE_OFFER_LIST";


    /**
     * Passenger details list Intent.extra key.
     */
    private static final String EXTRA_PASSENGER_DETAILS = "PASSENGER_DETAILS";


    /**
     * Ride preferences list Intent.extra key.
     */
    private static final String EXTRA_RIDE_PREFERENCES = "RIDE_PREFERENCES";


    /**
     * Use DemandClient to request a ride.
     */
    private DemandClient demandClient;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_offers);
        demandClient = DemandClient.newInstance(this);
        updateUI();
    }


    /**
     * Update UI
     */
    private void updateUI() {
        RecyclerView rideOffersList = findViewById(R.id.ride_offers_list);
        RideOffersAdapter adapter = new RideOffersAdapter(this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        rideOffersList.setLayoutManager(layoutManager);
        rideOffersList.setItemAnimator(new DefaultItemAnimator());
        rideOffersList.setAdapter(adapter);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.show_offers_title);
        }

        //Received ride offers list from Intent.extra and update the list.
        ArrayList<RideOffer> rideOffers = getRideOffers();
        adapter.updateDataSource(rideOffers);
    }


    /**
     * Called when a ride offer was selected.
     * @param The selected {@link RideOffer}.
     */
    @Override
    public void offerItemSelected(@NonNull RideOffer offer) {
        offer.accept(new RideOffer.Visitor<Void>() {
            @Override
            public Void visit(@NonNull TaxiRideOffer taxiRideOffer) {
                requestRide(taxiRideOffer);
                return null;
            }


            @Override
            public Void visit(@NonNull PublicTransportRideOffer publicTransportRideOffer) {
                startActivity(PublicTransportActivity.createIntent(RideOffersActivity.this, publicTransportRideOffer));
                return null;
            }
        });
    }


    /**
     * Request to book a Ride Offer.
     * @param taxiRideOffer A taxi ride offer, received from RideOffersRequest.
     */
    private void requestRide(@NonNull TaxiRideOffer taxiRideOffer) {
        PassengerDetails passengerDetails = getPassengerDetails();

        if (passengerDetails != null) {
            CreateRideRequest.Builder rideRequestBuilder = CreateRideRequest.builder(taxiRideOffer.getOfferId(), passengerDetails);

            RidePreferences ridePreferences = getRidePreferences();
            if (ridePreferences != null) {
                rideRequestBuilder.setRidePreferences(ridePreferences);
            }

            //Request to book a ride.
            ResponseFuture<Ride> rideRequestFuture = demandClient.createRide(rideRequestBuilder.build());

            //Register for ride request updates.
            rideRequestFuture.registerListener(rideFutureListener);
        }
    }


    /**
     * Future ride listener.
     */
    private ResponseListener<Ride> rideFutureListener = new ResponseListener<Ride>() {
        @Override
        public void onResponse(Ride ride) {
            startActivity(RideStatusActivity.createIntent(RideOffersActivity.this, ride));
            finish();
        }


        @Override
        public void onError(@NonNull ResponseException e) {
            Toast.makeText(RideOffersActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();

        //It's important to call shutdown function when the client is no longer needed.
        if (demandClient != null) {
            demandClient.shutdownNow();
        }
    }


    /**
     * Getter. Extra list of ride offers
     *
     * @return list of ride offers
     * @throws RuntimeException if RideOffer list is empty or null.
     */
    @NonNull
    private ArrayList<RideOffer> getRideOffers() {
        ArrayList<RideOffer> rideOffers = Lists.newArrayList();
        if (getIntent().hasExtra(EXTRA_TAXI_RIDE_OFFER_LIST)) {
            rideOffers.addAll(getIntent().getParcelableArrayListExtra(EXTRA_TAXI_RIDE_OFFER_LIST));
        }
        if (getIntent().hasExtra(EXTRA_PT_RIDE_OFFER_LIST)) {
            rideOffers.addAll(getIntent().getParcelableArrayListExtra(EXTRA_PT_RIDE_OFFER_LIST));
        }

        if (rideOffers.size() == 0) {
            throw new RuntimeException("Ride offer list is mandatory for starting RideOffersActivity");
        }
        return rideOffers;
    }


    /**
     * Gets PassengerDetails.
     * @return PassengerDetails
     */
    @Nullable
    private PassengerDetails getPassengerDetails() {
        PassengerDetails passengerDetails = null;
        if (getIntent().hasExtra(EXTRA_PASSENGER_DETAILS)) {
            passengerDetails = getIntent().getParcelableExtra(EXTRA_PASSENGER_DETAILS);
        }
        return passengerDetails;
    }


    /**
     * Getter. Extra RidePreferences.
     *
     * @return ridePreferences Optional, RidePreferences.
     */
    @Nullable
    private RidePreferences getRidePreferences() {
        RidePreferences ridePreferences = null;
        if (getIntent().hasExtra(EXTRA_RIDE_PREFERENCES)) {
            ridePreferences = getIntent().getParcelableExtra(EXTRA_RIDE_PREFERENCES);
        }
        return ridePreferences;
    }


    /**
     * A Helper method. The main task of this Activity is to request Ride Offers and book the selected offer.
     * PassengerDetails are needed to create a ride after an offer is selected.
     * @param context The context of the sender.
     * @param taxiRideOffers list of TaxiRideOffer objects, received from {@link DemandClient#getRideOffers(RideOffersRequest)}
     * @param ptRideRequest list of PublicTransportRideOffer objects, received from {@link DemandClient#getRideOffers(RideOffersRequest)}
     * @param passengerDetails PassengerDetails of user.
     * @param ridePreferences  preferences of the ride (e.g. should receive SMS on ride updates)
     * @return An Intent to RideOffersActivity with safe pass parameters.
     */
    @NonNull
    public static Intent createIntent(Context context,
                                      @NonNull ArrayList<TaxiRideOffer> taxiRideOffers,
                                      @NonNull ArrayList<PublicTransportRideOffer> ptRideRequest,
                                      @NonNull PassengerDetails passengerDetails,
                                      @Nullable RidePreferences ridePreferences) {
        Intent intent = new Intent(context, RideOffersActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_TAXI_RIDE_OFFER_LIST, taxiRideOffers);
        intent.putParcelableArrayListExtra(EXTRA_PT_RIDE_OFFER_LIST, ptRideRequest);
        intent.putExtra(EXTRA_PASSENGER_DETAILS, passengerDetails);
        intent.putExtra(EXTRA_RIDE_PREFERENCES, ridePreferences);
        return intent;
    }
}