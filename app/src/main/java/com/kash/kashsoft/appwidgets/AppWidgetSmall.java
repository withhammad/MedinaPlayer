package com.kash.kashsoft.appwidgets;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;
import com.kash.kashsoft.appwidgets.base.BaseAppWidget;
import com.kash.kashsoft.glide.SongGlideRequest;
import com.kash.kashsoft.glide.palette.BitmapPaletteWrapper;
import com.kash.kashsoft.model.Song;
import com.kash.kashsoft.service.MusicService;
import com.kash.kashsoft.ui.activities.MainActivity;
import com.kash.kashsoft.util.Util;

public class AppWidgetSmall extends BaseAppWidget {
    public static final String NAME = "app_widget_small";

    private static AppWidgetSmall mInstance;
    private static int imageSize = 0;
    private static float cardRadius = 0f;
    private Target<BitmapPaletteWrapper> target; // for cancellation

    public static synchronized AppWidgetSmall getInstance() {
        if (mInstance == null) {
            mInstance = new AppWidgetSmall();
        }
        return mInstance;
    }

    /**
     * Initialize given widgets to default state, where we launch Music on
     * default click and hide actions if service not running.
     */
    protected void defaultAppWidget(final Context context, final int[] appWidgetIds) {
        final RemoteViews appWidgetView = new RemoteViews(context.getPackageName(), com.kash.kashsoft.R.layout.app_widget_small);

        appWidgetView.setViewVisibility(com.kash.kashsoft.R.id.media_titles, View.INVISIBLE);
        appWidgetView.setImageViewResource(com.kash.kashsoft.R.id.image, com.kash.kashsoft.R.drawable.default_album_art);
        appWidgetView.setImageViewBitmap(com.kash.kashsoft.R.id.button_next, createBitmap(Util.getTintedVectorDrawable(context, com.kash.kashsoft.R.drawable.ic_skip_next_white_24dp, MaterialValueHelper.getSecondaryTextColor(context, true)), 1f));
        appWidgetView.setImageViewBitmap(com.kash.kashsoft.R.id.button_prev, createBitmap(Util.getTintedVectorDrawable(context, com.kash.kashsoft.R.drawable.ic_skip_previous_white_24dp, MaterialValueHelper.getSecondaryTextColor(context, true)), 1f));
        appWidgetView.setImageViewBitmap(com.kash.kashsoft.R.id.button_toggle_play_pause, createBitmap(Util.getTintedVectorDrawable(context, com.kash.kashsoft.R.drawable.ic_play_arrow_white_24dp, MaterialValueHelper.getSecondaryTextColor(context, true)), 1f));

        linkButtons(context, appWidgetView);
        pushUpdate(context, appWidgetIds, appWidgetView);
    }

    /**
     * Update all active widget instances by pushing changes
     */
    public void performUpdate(final MusicService service, final int[] appWidgetIds) {
        final RemoteViews appWidgetView = new RemoteViews(service.getPackageName(), com.kash.kashsoft.R.layout.app_widget_small);

        final boolean isPlaying = service.isPlaying();
        final Song song = service.getCurrentSong();

        // Set the titles and artwork
        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName)) {
            appWidgetView.setViewVisibility(com.kash.kashsoft.R.id.media_titles, View.INVISIBLE);
        } else {
            if (TextUtils.isEmpty(song.title) || TextUtils.isEmpty(song.artistName)) {
                appWidgetView.setTextViewText(com.kash.kashsoft.R.id.text_separator, "");
            } else {
                appWidgetView.setTextViewText(com.kash.kashsoft.R.id.text_separator, "•");
            }

            appWidgetView.setViewVisibility(com.kash.kashsoft.R.id.media_titles, View.VISIBLE);
            appWidgetView.setTextViewText(com.kash.kashsoft.R.id.title, song.title);
            appWidgetView.setTextViewText(com.kash.kashsoft.R.id.text, song.artistName);
        }

        // Link actions buttons to intents
        linkButtons(service, appWidgetView);

        if (imageSize == 0)
            imageSize = service.getResources().getDimensionPixelSize(com.kash.kashsoft.R.dimen.app_widget_small_image_size);
        if (cardRadius == 0f)
            cardRadius = service.getResources().getDimension(com.kash.kashsoft.R.dimen.app_widget_card_radius);

        // Load the album cover async and push the update on completion
        final Context appContext = service.getApplicationContext();
        service.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (target != null) {
                    Glide.clear(target);
                }
                target = SongGlideRequest.Builder.from(Glide.with(appContext), song)
                        .checkIgnoreMediaStore(appContext)
                        .generatePalette(service).build()
                        .centerCrop()
                        .into(new SimpleTarget<BitmapPaletteWrapper>(imageSize, imageSize) {
                            @Override
                            public void onResourceReady(BitmapPaletteWrapper resource, GlideAnimation<? super BitmapPaletteWrapper> glideAnimation) {
                                Palette palette = resource.getPalette();
                                update(resource.getBitmap(), palette.getVibrantColor(palette.getMutedColor(MaterialValueHelper.getSecondaryTextColor(appContext, true))));
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                super.onLoadFailed(e, errorDrawable);
                                update(null, MaterialValueHelper.getSecondaryTextColor(appContext, true));
                            }

                            private void update(@Nullable Bitmap bitmap, int color) {
                                // Set correct drawable for pause state
                                int playPauseRes = isPlaying ? com.kash.kashsoft.R.drawable.ic_pause_white_24dp : com.kash.kashsoft.R.drawable.ic_play_arrow_white_24dp;
                                appWidgetView.setImageViewBitmap(com.kash.kashsoft.R.id.button_toggle_play_pause, createBitmap(Util.getTintedVectorDrawable(service, playPauseRes, color), 1f));

                                // Set prev/next button drawables
                                appWidgetView.setImageViewBitmap(com.kash.kashsoft.R.id.button_next, createBitmap(Util.getTintedVectorDrawable(service, com.kash.kashsoft.R.drawable.ic_skip_next_white_24dp, color), 1f));
                                appWidgetView.setImageViewBitmap(com.kash.kashsoft.R.id.button_prev, createBitmap(Util.getTintedVectorDrawable(service, com.kash.kashsoft.R.drawable.ic_skip_previous_white_24dp, color), 1f));

                                final Drawable image = getAlbumArtDrawable(service.getResources(), bitmap);
                                final Bitmap roundedBitmap = createRoundedBitmap(image, imageSize, imageSize, cardRadius, 0, 0, 0);
                                appWidgetView.setImageViewBitmap(com.kash.kashsoft.R.id.image, roundedBitmap);

                                pushUpdate(appContext, appWidgetIds, appWidgetView);
                            }
                        });
            }
        });
    }

    /**
     * Link up various button actions using {@link PendingIntent}.
     */
    private void linkButtons(final Context context, final RemoteViews views) {
        Intent action;
        PendingIntent pendingIntent;

        final ComponentName serviceName = new ComponentName(context, MusicService.class);

        // Home
        action = new Intent(context, MainActivity.class);
        action.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pendingIntent = PendingIntent.getActivity(context, 0, action, 0);
        views.setOnClickPendingIntent(com.kash.kashsoft.R.id.image, pendingIntent);
        views.setOnClickPendingIntent(com.kash.kashsoft.R.id.media_titles, pendingIntent);

        // Previous track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_REWIND, serviceName);
        views.setOnClickPendingIntent(com.kash.kashsoft.R.id.button_prev, pendingIntent);

        // Play and pause
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_TOGGLE_PAUSE, serviceName);
        views.setOnClickPendingIntent(com.kash.kashsoft.R.id.button_toggle_play_pause, pendingIntent);

        // Next track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_SKIP, serviceName);
        views.setOnClickPendingIntent(com.kash.kashsoft.R.id.button_next, pendingIntent);
    }
}
