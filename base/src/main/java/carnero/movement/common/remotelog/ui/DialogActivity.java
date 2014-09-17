/*
 * Copyright (c) 2009-2013, Inmite s.r.o. (www.inmite.eu). All rights reserved.
 *
 * This source code can be used only for purposes specified by the given license contract
 * signed by the rightful deputy of Inmite s.r.o. This source code can be used only
 * by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package carnero.movement.common.remotelog.ui;

import java.io.File;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import carnero.movement.common.Application;
import carnero.movement.common.R;
import carnero.movement.common.remotelog.Prefs;
import carnero.movement.common.remotelog.RemoteLog;

/**
 * @author carnero
 */
public class DialogActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final File file = new File(Environment.getExternalStorageDirectory(), RemoteLog.LOG_FILE);
        if (!Application.isDebug() || !file.exists() || file.length() == 0) {
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.remotelog_dialog);

        Button cancel = (Button)findViewById(R.id.btn_negative);
        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                file.delete();

                Prefs.saveLastEmail(System.currentTimeMillis());
                finish();
            }
        });

        Button download = (Button)findViewById(R.id.btn_positive);
        download.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                PackageInfo pkgInfo = null;
                try {
                    pkgInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    // pokemon
                }

                StringBuilder info = new StringBuilder();
                info.append("Application: ");
                info.append(getString(R.string.app_name));

                if (pkgInfo != null) {
                    info.append("\n");

                    info.append("- package: ");
                    info.append(pkgInfo.packageName);
                    info.append("\n");

                    info.append("- version: ");
                    info.append(pkgInfo.versionName);
                    info.append(" / ");
                    info.append(pkgInfo.versionCode);
                    info.append("\n");

                    info.append("- updated: ");
                    info.append(new Date(pkgInfo.lastUpdateTime).toString());
                }

                info.append("\n\n");
                info.append("Something developers should to know? > ");

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"carnero@inmite.eu"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Application log: " + getString(R.string.app_name));
                intent.putExtra(Intent.EXTRA_TEXT, info.toString());
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

                startActivity(Intent.createChooser(intent, "Send logs..."));

                Prefs.saveLastEmail(System.currentTimeMillis());
                finish();
            }
        });
    }
}