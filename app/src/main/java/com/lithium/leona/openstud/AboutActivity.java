package com.lithium.leona.openstud;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.danielstone.materialaboutlibrary.ConvenienceBuilder;
import com.danielstone.materialaboutlibrary.MaterialAboutActivity;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

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
        super.onCreate(savedInstanceState);
    }

    private void buildApp(Context context, MaterialAboutCard.Builder appCardBuilder) {
        appCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getResources().getString(R.string.version))
                .icon(ContextCompat.getDrawable(context, R.drawable.ic_update_black)).subText(BuildConfig.VERSION_NAME).build());

    }

    private void buildAuthor(Context context, MaterialAboutCard.Builder authorCardBuilder) {
        authorCardBuilder.title(R.string.author);
        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text("Leonardo Sarra")
                .subText("LithiumSR")
                .icon(ContextCompat.getDrawable(context, R.drawable.ic_person_outline_black))
                .build())
                .addItem(new MaterialAboutActionItem.Builder()
                        .text(R.string.fork_github)
                        .icon(ContextCompat.getDrawable(context, R.drawable.ic_github_circle))
                        .setOnClickAction( ()-> startActivity(createBrowserLink("https://www.github.com/lithiumSR/openstud_client")))
                        .build())
                .addItem(ConvenienceBuilder.createEmailItem(context, ContextCompat.getDrawable(context, R.drawable.ic_email_black),
                        getString(R.string.send_email), true, getString(R.string.email_address), getString(R.string.question_concerning_openstud)));
    }

    private void buildMisc(Context context, MaterialAboutCard.Builder miscCardBuilder) {
        miscCardBuilder.title(R.string.about).addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.open_source_libs)
                .icon(ContextCompat.getDrawable(context, R.drawable.ic_github_circle))
                .setOnClickAction(() -> new LibsBuilder()
                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                        .withLibraries("openstud")
                        .withActivityTheme(R.style.NoActionBarAppTheme)
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
}