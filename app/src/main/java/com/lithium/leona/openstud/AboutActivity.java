package com.lithium.leona.openstud;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;

import com.danielstone.materialaboutlibrary.ConvenienceBuilder;
import com.danielstone.materialaboutlibrary.MaterialAboutActivity;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;

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
        return new MaterialAboutList(appCardBuilder.build(),authorCardBuilder.build(),miscCardBuilder.build());
    }

    @Nullable
    @Override
    protected CharSequence getActivityTitle() {
        return "OpenStud";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_MaterialAboutActivityDark);
        super.onCreate(savedInstanceState);
    }

    private void buildApp(Context context, MaterialAboutCard.Builder appCardBuilder) {
        int tintColor = getPrimaryTextColor(this);
        Drawable version = ContextCompat.getDrawable(context, R.drawable.ic_update_black);
        version.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);

        appCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getResources().getString(R.string.version))
                .icon(version).subText(BuildConfig.VERSION_NAME).build());

    }

    private void buildAuthor(Context context, MaterialAboutCard.Builder authorCardBuilder) {
        int tintColor = getPrimaryTextColor(this);
        Drawable person = ContextCompat.getDrawable(context, R.drawable.ic_person_outline_black);
        Drawable github = new IconicsDrawable(this)
                .icon(FontAwesome.Icon.faw_github)
                .color(tintColor)
                .sizeDp(24);
        Drawable email = ContextCompat.getDrawable(context, R.drawable.ic_email_black);
        github.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        email.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        person.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        authorCardBuilder.title(R.string.author);
        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text("Leonardo Sarra")
                .subText("LithiumSR")
                .icon(person)
                .build())
                .addItem(new MaterialAboutActionItem.Builder()
                        .text(R.string.fork_github)
                        .icon(github)
                        .setOnClickAction( ()-> startActivity(createBrowserLink("https://www.github.com/lithiumSR/openstud_client")))
                        .build())
                .addItem(ConvenienceBuilder.createEmailItem(context, email,
                        getString(R.string.send_email), true, getString(R.string.email_address), getString(R.string.question_concerning_openstud)));
    }

    private void buildMisc(Context context, MaterialAboutCard.Builder miscCardBuilder) {
        int tintColor = getPrimaryTextColor(this);
        Drawable github = new IconicsDrawable(this)
                .icon(FontAwesome.Icon.faw_github)
                .color(tintColor)
                .sizeDp(24);
        miscCardBuilder.title(R.string.about).addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.open_source_libs)
                .icon(github)
                .setOnClickAction(() -> new LibsBuilder()
                        .withActivityStyle(Libs.ActivityStyle.DARK)
                        .withAutoDetect(true)
                        .withActivityTitle(this.getResources().getString(R.string.open_source_libs))
                        .withAboutIconShown(true)
                        .withAboutVersionShown(true)
                        .start(this))
                .build());
    }
    private Intent createBrowserLink(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        return i;
    }

    private int getPrimaryTextColor(Context context){
        int tintColor;
        TypedValue tV = new TypedValue();
        Resources.Theme theme = context.getTheme();
        boolean success = theme.resolveAttribute(R.attr.primaryTextColor, tV, true);
        if (success) tintColor = tV.data;
        else tintColor = ContextCompat.getColor(context, android.R.color.black);
        return tintColor;
    }
}