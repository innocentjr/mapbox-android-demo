package com.mapbox.mapboxandroiddemo.labs;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

public class InfoWindowSymbolLayerActivity extends AppCompatActivity implements
  OnMapReadyCallback, MapboxMap.OnMapClickListener {

  private MapView mapView;
  private MapboxMap mapboxMap;
  private boolean markerSelected = false;
  private FeatureCollection mapLocationFeatureCollection;
  private HashMap<String, View> viewMap;
  private GeoJsonSource mapLocationsGeoJsonSource;
  private String GEOJSON_SOURCE_ID = "GEOJSON_SOURCE_ID";
  private String MARKER_IMAGE_ID = "MARKER_IMAGE_ID";
  private String MARKER_ICON_LOCATION_LAYER_ID = "MARKER_ICON_LOCATION_LAYER_ID";
  private String FEATURE_TITLE_PROPERTY_KEY = "FEATURE_TITLE_PROPERTY_KEY";
  private String FEATURE_DESCRIPTION_PROPERTY_KEY = "FEATURE_DESCRIPTION_PROPERTY_KEY";
  private String TAG = "InfoWindowSymbolLayerActivity";
  private AnimatorSet animatorSet;

  private static final long CAMERA_ANIMATION_TIME = 1950;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_info_window_symbol_layer);

    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {

    this.mapboxMap = mapboxMap;

    // Create list of Feature objects
    List<Feature> mapLocationCoordinates = new ArrayList<>();

    // Create a single Feature location in Caracas, Venezuela
    Feature singleFeature = Feature.fromGeometry(Point.fromLngLat(
      -66.910519, 10.503250));

    // Add a String property to the Feature to be used in the title of the popup bubble window
    singleFeature.addStringProperty(FEATURE_TITLE_PROPERTY_KEY, "Hello World!");
    singleFeature.addStringProperty(FEATURE_DESCRIPTION_PROPERTY_KEY, "Welcome to my marker");

    // Add the Feature to the List<> of Feature objects
    mapLocationCoordinates.add(singleFeature);

    // Add the list as a parameter to create a FeatureCollection
    mapLocationFeatureCollection = FeatureCollection.fromFeatures(mapLocationCoordinates);

    // Create a GeoJSON source with a unique ID and a FeatureCollection
    mapLocationsGeoJsonSource = new GeoJsonSource(GEOJSON_SOURCE_ID, mapLocationFeatureCollection);

    // Add the GeoJSON source to the map
    mapboxMap.addSource(mapLocationsGeoJsonSource);

    // Create a bitmap that will serve as the visual marker icon image
    Bitmap redMarkerIcon = BitmapFactory.decodeResource(
      InfoWindowSymbolLayerActivity.this.getResources(), R.drawable.red_marker);

    // Add the marker icon image to the map
    mapboxMap.addImage(MARKER_IMAGE_ID, redMarkerIcon);

    // Create a SymbolLayer with a unique id and a source. In this case, it's the GeoJSON source
    // that was created above. The red marker icon is added to the layer using run-time styling.
    SymbolLayer mapLocationSymbolLayer = new SymbolLayer(MARKER_ICON_LOCATION_LAYER_ID, GEOJSON_SOURCE_ID)
      .withProperties(iconImage(MARKER_IMAGE_ID));
    mapboxMap.addLayer(mapLocationSymbolLayer);

    // Start the async task that creates the actual popup bubble window. This window will appear once a
    // SymbolLayer icon is tapped on.
    new GenerateViewIconTask(this).execute(mapLocationFeatureCollection);

    // Initialize the map click listener
    mapboxMap.addOnMapClickListener(this);
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    final SymbolLayer symbolIconLayer = (SymbolLayer) mapboxMap.getLayer(MARKER_ICON_LOCATION_LAYER_ID);
    final PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
    List<Feature> markerIconFeatures = mapboxMap.queryRenderedFeatures(screenPoint, MARKER_ICON_LOCATION_LAYER_ID);
    Log.d(TAG, "onMapClick: markerIconFeatures.size = " + markerIconFeatures.size());
    if (markerIconFeatures.isEmpty()) {
      Log.d(TAG, "onMapClick: markerIconFeatures.isEmpty()");
      if (markerSelected) {
        Log.d(TAG, "onMapClick: markerSelected");
        // TODO: Finish runDeselectMarkerIconAnimation()
        runDeselectMarkerIconAnimation(symbolIconLayer);
      }
      return;
    }

    /*FeatureCollection featureCollection = FeatureCollection.fromFeatures(markerIconFeatures);
    GeoJsonSource source = mapboxMap.getSourceAs(GEOJSON_SOURCE_ID);
    if (source != null) {
      source.setGeoJson(featureCollection);
    }*/

    if (!markerIconFeatures.isEmpty()) {
      Log.d(TAG, "onMapClick: !markerIconFeatures.isEmpty()");

      Log.d(TAG, "onMapClick: markerIconFeatures.get(0).toString() = " + markerIconFeatures.get(0).toString());

      handleClickIcon(screenPoint);


      /*
      String title = markerIconFeatures.get(0).getStringProperty(FEATURE_TITLE_PROPERTY_KEY);
      Log.d(TAG, "onMapClick: title = " + title);
      List<Feature> featureList = mapLocationFeatureCollection.features();
      Log.d(TAG, "onMapClick: featureList.size() = " + featureList.size());
      for (int i = 0; i < featureList.size(); i++) {
        Log.d(TAG, "onMapClick: i = " + i);
        if (featureList.get(i).getStringProperty(FEATURE_TITLE_PROPERTY_KEY).equals(title)) {
          Log.d(TAG, "onMapClick: featureList.get(i).getStringProperty(FEATURE_TITLE_PROPERTY_KEY).equals(title)");
          setSelected(i, true);
        }
      }*/
    }
  }

  /**
   * Set a feature selected state with the ability to scroll the RecycleViewer to the provided index.
   *
   * @param index      the index of selected feature
   * @param withScroll indicates if the recyclerView position should be updated
   *//*
  private void setSelected(int index, boolean withScroll) {

    deselectAll(false);

    Feature feature = featureCollection.features().get(index);
    selectFeature(feature);
    animateCameraToSelection(feature);
    refreshSource();
    loadMapillaryData(feature);

  }*/

  /**
   * This method handles click events for maki symbols.
   * <p>
   * When a maki symbol is clicked, we moved that feature to the selected state.
   * </p>
   *
   * @param screenPoint the point on screen clicked
   */
  private void handleClickIcon(PointF screenPoint) {
    Log.d(TAG, "handleClickIcon: ");
    List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, MARKER_ICON_LOCATION_LAYER_ID);
    Log.d(TAG, "handleClickIcon: features.size = " + features.size());
    if (!features.isEmpty()) {

      Log.d(TAG, "handleClickIcon: !features.isEmpty()");
      String title = features.get(0).getStringProperty(FEATURE_TITLE_PROPERTY_KEY);
      Log.d(TAG, "handleClickIcon: title = " + title);

      List<Feature> featureList = mapLocationFeatureCollection.features();
      Log.d(TAG, "handleClickIcon: featureList size = " + featureList.size());

      for (int i = 0; i < featureList.size(); i++) {
        if (featureList.get(i).getStringProperty(FEATURE_TITLE_PROPERTY_KEY).equals(title)) {
          setSelected(i);
        }
      }
    }
  }

  /**
   * Set a feature selected state with the ability to scroll the RecycleViewer to the provided index.
   *
   * @param index the index of selected feature
   */
  private void setSelected(int index) {
    Feature feature = mapLocationFeatureCollection.features().get(index);

    String title = feature.getStringProperty(FEATURE_TITLE_PROPERTY_KEY);
    Log.d(TAG, "setSelected: feature.getStringProperty(FEATURE_TITLE_PROPERTY_KEY) = "
      + feature.getStringProperty(FEATURE_TITLE_PROPERTY_KEY));

    Log.d(TAG, "setSelected: viewMap " + viewMap == null ? "true" : "fals");
    View view = viewMap.get(title);

    Bitmap bitmap = SymbolGenerator.generate(view);
    mapboxMap.addImage(title, bitmap);
    refreshSource();
  }

  private LatLng convertToLatLng(Feature feature) {
    Point symbolPoint = (Point) feature.geometry();
    return new LatLng(symbolPoint.coordinates().get(1), symbolPoint.coordinates().get(1));
  }

  private void runSelectMarkerIconAnimation(final SymbolLayer symbolLayer) {
    ValueAnimator markerAnimator = new ValueAnimator();
    markerAnimator.setObjectValues(1f, 2f);
    markerAnimator.setDuration(300);
    markerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animator) {
        symbolLayer.setProperties(
          PropertyFactory.iconSize((float) animator.getAnimatedValue())
        );
      }
    });
    markerAnimator.start();
    markerSelected = true;
  }

  private void runDeselectMarkerIconAnimation(final SymbolLayer symbolLayer) {
    ValueAnimator markerAnimator = new ValueAnimator();
    markerAnimator.setObjectValues(2f, 1f);
    markerAnimator.setDuration(300);
    markerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animator) {
        symbolLayer.setProperties(
          PropertyFactory.iconSize((float) animator.getAnimatedValue())
        );
      }
    });
    markerAnimator.start();
    markerSelected = false;
  }

  /**
   * This method handles click events for callout symbols.
   * <p>
   * It creates a hit rectangle based on the the textView, offsets that rectangle to the location
   * of the symbol on screen and hit tests that with the screen point.
   * </p>
   *
   * @param feature           the feature that was clicked
   * @param screenPoint       the point on screen clicked
   * @param symbolScreenPoint the point of the symbol on screen
   */
  private void handleClickCallout(Feature feature, PointF screenPoint, PointF symbolScreenPoint) {
    if (viewMap != null) {
      Log.d(TAG, "handleClickCallout: viewMap != null");

      View view = viewMap.get(feature.getStringProperty(FEATURE_TITLE_PROPERTY_KEY));
      View textContainer = view.findViewById(R.id.plain_text_container);

      // create hit box for textView
      Rect hitRectText = new Rect();
      textContainer.getHitRect(hitRectText);

      // move hit box to location of symbol
      hitRectText.offset((int) symbolScreenPoint.x, (int) symbolScreenPoint.y);

      // offset vertically to match anchor behaviour
      hitRectText.offset(0, -view.getMeasuredHeight());


      // hit test if clicked point is in textview hit box
      if (!hitRectText.contains((int) screenPoint.x, (int) screenPoint.y)) {
        List<Feature> featureList = mapLocationFeatureCollection.features();
        for (int i = 0; i < featureList.size(); i++) {
          if (featureList.get(i).getStringProperty(FEATURE_TITLE_PROPERTY_KEY).equals(feature.getStringProperty(FEATURE_TITLE_PROPERTY_KEY))) {
          }
        }
      }
    } else {
      Log.d(TAG, "handleClickCallout: viewmap == null");
    }

    View view = viewMap.get(feature.getStringProperty(FEATURE_TITLE_PROPERTY_KEY));
    View textContainer = view.findViewById(R.id.plain_text_container);

    // create hitbox for textView
    Rect hitRectText = new Rect();
    textContainer.getHitRect(hitRectText);

    // move hitbox to location of symbol
    hitRectText.offset((int) symbolScreenPoint.x, (int) symbolScreenPoint.y);

    // offset vertically to match anchor behaviour
    hitRectText.offset(0, -view.getMeasuredHeight());

    // hit test if clicked point is in textview hitbox
    if (hitRectText.contains((int) screenPoint.x, (int) screenPoint.y)) {
      // user clicked on text
      String callout = feature.getStringProperty("call-out");
      Toast.makeText(this, callout, Toast.LENGTH_LONG).show();
    }
  }

  private void refreshSource() {
    if (mapLocationsGeoJsonSource != null && mapLocationFeatureCollection != null) {
      Log.d(TAG, "refreshSource: mapLocationsGeoJsonSource != null && mapLocationFeatureCollection != null");
      mapLocationsGeoJsonSource.setGeoJson(mapLocationFeatureCollection);
    }
  }

  /**
   * AsyncTask to generate Bitmap from Views to be used as iconImage in a SymbolLayer.
   * <p>
   * Call be optionally be called to update the underlying data source after execution.
   * </p>
   * <p>
   * Generating Views on background thread since we are not going to be adding them to the view hierarchy.
   * </p>
   */
  private static class GenerateViewIconTask extends AsyncTask<FeatureCollection, Void, HashMap<String, Bitmap>> {
    private final HashMap<String, View> viewMap = new HashMap<>();
    private final WeakReference<InfoWindowSymbolLayerActivity> activityRef;
    private final boolean refreshSource;


    GenerateViewIconTask(InfoWindowSymbolLayerActivity activity, boolean refreshSource) {
      this.activityRef = new WeakReference<>(activity);
      this.refreshSource = refreshSource;
    }

    GenerateViewIconTask(InfoWindowSymbolLayerActivity activity) {
      this(activity, false);
    }


    @SuppressWarnings("WrongThread")
    @Override
    protected HashMap<String, Bitmap> doInBackground(FeatureCollection... params) {
      InfoWindowSymbolLayerActivity activity = activityRef.get();
      if (activity != null) {
        HashMap<String, Bitmap> imagesMap = new HashMap<>();
        LayoutInflater inflater = LayoutInflater.from(activity);
        FeatureCollection featureCollection = params[0];

        for (Feature feature : featureCollection.features()) {
          View view = inflater.inflate(R.layout.symbol_layer_info_window_layout_callout, null);

          String titleForBubbleWindow = feature.getStringProperty("FEATURE_TITLE_PROPERTY_KEY");
          TextView titleNumTextView = view.findViewById(R.id.plain_title);
          titleNumTextView.setText(titleForBubbleWindow);

          String descriptionForBubbleWindow = feature.getStringProperty("FEATURE_DESCRIPTION_PROPERTY_KEY");
          TextView descriptionNumTextView = view.findViewById(R.id.plain_description);
          descriptionNumTextView.setText(descriptionForBubbleWindow);

          Bitmap bitmap = SymbolGenerator.generate(view);
          imagesMap.put(titleForBubbleWindow, bitmap);
          viewMap.put(titleForBubbleWindow, view);
        }
        return imagesMap;
      } else {
        return null;
      }
    }

    @Override
    protected void onPostExecute(HashMap<String, Bitmap> bitmapHashMap) {
      super.onPostExecute(bitmapHashMap);

      InfoWindowSymbolLayerActivity activity = activityRef.get();
      if (activity != null && bitmapHashMap != null) {
        activity.setImageGenResults(viewMap, bitmapHashMap);
        if (refreshSource) {
          activity.refreshSource();
        }
      }
    }
  }

  /**
   * Invoked when the bitmaps have been generated from a view.
   */
  public void setImageGenResults(HashMap<String, View> viewMap, HashMap<String, Bitmap> imageMap) {
    if (mapboxMap != null) {
      // calling addImages is faster as separate addImage calls for each bitmap.
      mapboxMap.addImages(imageMap);
      Log.d(TAG, "setImageGenResults: images added");
    } else {
      Log.d(TAG, "setImageGenResults: mapboxMapForViewGeneration == null");
    }
    // need to store reference to views to be able to use them as hit boxes for click events.
    this.viewMap = viewMap;
  }

  /**
   * Utility class to generate Bitmaps for Symbol.
   * <p>
   * Bitmaps can be added to the map with {@link com.mapbox.mapboxsdk.maps.MapboxMap#addImage(String, Bitmap)}
   * </p>
   */
  private static class SymbolGenerator {

    /**
     * Generate a Bitmap from an Android SDK View.
     *
     * @param view the View to be drawn to a Bitmap
     * @return the generated bitmap
     */
    static Bitmap generate(@NonNull View view) {
      int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
      view.measure(measureSpec, measureSpec);

      int measuredWidth = view.getMeasuredWidth();
      int measuredHeight = view.getMeasuredHeight();

      view.layout(0, 0, measuredWidth, measuredHeight);
      Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
      bitmap.eraseColor(Color.TRANSPARENT);
      Canvas canvas = new Canvas(bitmap);
      view.draw(canvas);
      return bitmap;
    }

  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mapboxMap != null) {
      mapboxMap.removeOnMapClickListener(this);
    }
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}