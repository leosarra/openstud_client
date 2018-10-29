package com.lithium.leona.openstud.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.danielstone.materialaboutlibrary.ConvenienceBuilder;
import com.danielstone.materialaboutlibrary.MaterialAboutActivity;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.lithium.leona.openstud.BuildConfig;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.Objects;

public class AboutActivity extends MaterialAboutActivity {

    @NonNull
    @Override
    protected MaterialAboutList getMaterialAboutList(@NonNull Context context) {
        MaterialAboutCard.Builder appCardBuilder = new MaterialAboutCard.Builder();
        buildApp(context, appCardBuilder);
        MaterialAboutCard.Builder authorCardBuilder = new MaterialAboutCard.Builder();
        buildAuthor(context, authorCardBuilder);
        MaterialAboutCard.Builder miscCardBuilder = new MaterialAboutCard.Builder();
        buildMisc(context, miscCardBuilder);
        return new MaterialAboutList(appCardBuilder.build(), miscCardBuilder.build(), authorCardBuilder.build());
    }

    @Nullable
    @Override
    protected CharSequence getActivityTitle() {
        return "OpenStud";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeEngine.applyAboutTheme(this);
        super.onCreate(savedInstanceState);
    }

    private void buildApp(Context context, MaterialAboutCard.Builder appCardBuilder) {
        int tintColor = ThemeEngine.getPrimaryTextColor(this);
        Drawable version = ContextCompat.getDrawable(context, R.drawable.ic_update_black);
        Objects.requireNonNull(version).setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        appCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getResources().getString(R.string.version))
                .icon(version).subText(BuildConfig.VERSION_NAME).build());

    }

    private void buildAuthor(Context context, MaterialAboutCard.Builder authorCardBuilder) {
        int tintColor = ThemeEngine.getPrimaryTextColor(this);
        Drawable person = ContextCompat.getDrawable(context, R.drawable.ic_person_outline_black);
        Drawable email = ContextCompat.getDrawable(context, R.drawable.ic_email_black);
        Objects.requireNonNull(email).setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        Objects.requireNonNull(person).setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        authorCardBuilder.title(R.string.author);
        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text("Leonardo Sarra")
                .icon(person)
                .build())
                .addItem(ConvenienceBuilder.createEmailItem(context, email,
                        getString(R.string.send_email), true, getString(R.string.email_address), getString(R.string.question_concerning_openstud)));
    }

    private void buildMisc(Context context, MaterialAboutCard.Builder miscCardBuilder) {
        int tintColor = ThemeEngine.getPrimaryTextColor(this);
        Drawable libraries = ContextCompat.getDrawable(context, R.drawable.ic_extension_black_24dp);
        Drawable github = new IconicsDrawable(this)
                .icon(FontAwesome.Icon.faw_github)
                .color(tintColor)
                .sizeDp(18);
        Drawable heart = new IconicsDrawable(this)
                .icon(FontAwesome.Icon.faw_heart)
                .color(tintColor)
                .sizeDp(18);
        Objects.requireNonNull(heart).setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        Objects.requireNonNull(github).setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        Objects.requireNonNull(libraries).setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        int id_theme = ThemeEngine.getAboutTheme(this);
        miscCardBuilder.title(R.string.about)
                .addItem(new MaterialAboutActionItem.Builder()
                        .text(R.string.fork_github)
                        .icon(github)
                        .setOnClickAction(() -> ClientHelper.createCustomTab(this, "https://www.github.com/lithiumSR/openstud_client"))
                        .build())
                .addItem(new MaterialAboutActionItem.Builder()
                        .text(R.string.contributors)
                        .icon(heart)
                        .setOnClickAction(() -> startActivity(new Intent(this, ContributorsActivity.class)))
                        .build())
                .addItem(new MaterialAboutActionItem.Builder()
                        .text(R.string.open_source_libs)
                        .icon(libraries)
                        .setOnClickAction(() -> new LibsBuilder()
                                .withAutoDetect(true)
                                .withActivityTitle(this.getResources().getString(R.string.open_source_libs))
                                .withAboutIconShown(true)
                                .withAboutVersionShown(true)
                                .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                                .withActivityTheme(id_theme)
                                .start(this))
                        .build());

    }
}