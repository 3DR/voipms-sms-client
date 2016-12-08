/*
 * VoIP.ms SMS
 * Copyright (C) 2015-2016 Michael Kourlas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.kourlas.voipms_sms.notifications;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import net.kourlas.voipms_sms.activities.CustomizeActivity;

import net.kourlas.voipms_sms.R;
import net.kourlas.voipms_sms.activities.ActivityMonitor;
import net.kourlas.voipms_sms.activities.ConversationActivity;
import net.kourlas.voipms_sms.activities.ConversationQuickReplyActivity;
import net.kourlas.voipms_sms.activities.ConversationsActivity;
import net.kourlas.voipms_sms.db.Database;
import net.kourlas.voipms_sms.model.Message;
import net.kourlas.voipms_sms.preferences.Preferences;
import net.kourlas.voipms_sms.receivers.MarkAsReadReceiver;
import net.kourlas.voipms_sms.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class Notifications{
    private static Notifications instance = null;

    private final Context applicationContext;
    private final Database database;
    private final Preferences preferences;
    public static SharedPreferences prefs;
    private final Map<String, Integer> notificationIds;
    private int notificationIdCount;

    private Context nContext;
    private Notifications(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.database = Database.getInstance(applicationContext);
        this.preferences = Preferences.getInstance(applicationContext);

        this.notificationIds = new HashMap<>();
        this.notificationIdCount = 0;

    }

    public static Notifications getInstance(Context applicationContext) {
        if (instance == null) {
            instance = new Notifications(applicationContext);
        }
        return instance;
    }

    /**
     * Gets the IDs of the currently active notifications.
     *
     * @return The IDs of the currently active notifications.
     */
    public Map<String, Integer> getNotificationIds() {
        return notificationIds;
    }

    /**
     * Show notifications for new messages for the specified conversations.
     *
     * @param contacts A list of contacts.
     */
    public void showNotifications(List<String> contacts) {
        // Only show notifications if notifications are enabled
        if (!preferences.getNotificationsEnabled()) {
            return;
        }

        // Do not show notifications when the conversations view is open
        if (ActivityMonitor.getInstance().getCurrentActivity()
            instanceof ConversationsActivity)
        {
            return;
        }

        for (String contact : contacts) {
            Activity currentActivity = ActivityMonitor.getInstance()
                                                      .getCurrentActivity();
            // Do not show notifications for a contact when that contact's
            // conversation view is open
            if (currentActivity instanceof ConversationActivity
                && ((ConversationActivity) currentActivity).getContact()
                                                           .equals(
                                                               contact))
            {
                continue;
            }

            Map<String, String> shortTexts = new HashMap<>();
            Map<String, String> longTexts = new HashMap<>();
            List<Message> messages = database.getUnreadMessages(
                preferences.getDid(), contact);
            for (Message message : messages) {
                if (shortTexts.get(contact) != null) {
                    longTexts.put(contact, message.getText() + "\n"
                                           + longTexts.get(contact));
                } else {
                    shortTexts.put(contact, message.getText());
                    longTexts.put(contact, message.getText());
                }
            }
            for (Map.Entry<String, String> entry : shortTexts.entrySet()) {
                showNotification(entry.getKey(), entry.getValue(),
                                 longTexts.get(entry.getKey()));
            }
        }
    }

    /**
     * Shows a notification with the specified details.
     *
     * @param contact   The contact that the notification is from.
     * @param shortText The short form of the message text.
     * @param longText  The long form of the message text.
     */
    private void showNotification(String contact,
                                  String shortText,
                                  String longText)
    {

        String title = Utils.getContactName(applicationContext,
                                            contact);
        if (title == null) {
            title = Utils.getFormattedPhoneNumber(contact);
        }
        NotificationCompat.Builder notification =
            new NotificationCompat.Builder(applicationContext);
        notification.setContentTitle(title);
        notification.setContentText(shortText);
        notification.setSmallIcon(R.drawable.ic_chat_white_24dp);
        notification.setPriority(Notification.PRIORITY_HIGH);
        String notificationSound = Preferences.getInstance(
            applicationContext).getNotificationSound();
        if (!notificationSound.equals("")) {
            notification.setSound(Uri.parse(Preferences.getInstance(
                applicationContext).getNotificationSound()));
        }
        String num = Utils.getFormattedPhoneNumber(contact);
        Log.v("prior", num);


        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences("ledData", Context.MODE_PRIVATE);
        String cNm = sharedPreferences.getString(title, "3000");
        SharedPreferences sharedPref = applicationContext.getSharedPreferences("color", Context.MODE_PRIVATE);
        int clr = sharedPref.getInt(title, 0xFF02ffff);
        Log.v("saved rate of ", cNm + " for " + title);
        int ledSpeed = 3000;
        int color = 0xFF02ffff;

        if (cNm.equals("500") || cNm.equals("3000")){
            ledSpeed = Integer.parseInt(cNm);
            color = clr;
        }

        notification.setLights(color, ledSpeed, ledSpeed);

        if (Preferences.getInstance(applicationContext)
                       .getNotificationVibrateEnabled())
        {
            notification.setVibrate(new long[] {0, 250, 250, 250});
        } else {
            notification.setVibrate(new long[] {0});
        }
        notification.setColor(0xFF546e7a);
        notification.setAutoCancel(true);
        notification.setStyle(new NotificationCompat.BigTextStyle()
                                  .bigText(longText));

        Bitmap largeIconBitmap;
        try {
            largeIconBitmap = MediaStore.Images.Media.getBitmap(
                applicationContext.getContentResolver(), Uri.parse(
                    Utils.getContactPhotoUri(applicationContext, contact)));
            largeIconBitmap = Bitmap.createScaledBitmap(largeIconBitmap,
                                                        256, 256, false);
            largeIconBitmap = Utils.applyCircularMask(largeIconBitmap);
            notification.setLargeIcon(largeIconBitmap);
        } catch (Exception ignored) {
            // Do nothing.
        }

        Intent intent = new Intent(applicationContext,
                                   ConversationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(applicationContext.getString(
            R.string.conversation_extra_contact),
                        contact);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(
            applicationContext);
        stackBuilder.addParentStack(ConversationActivity.class);
        stackBuilder.addNextIntent(intent);
        notification.setContentIntent(stackBuilder.getPendingIntent(
            0, PendingIntent.FLAG_CANCEL_CURRENT));

        Intent replyIntent = new Intent(applicationContext,
                                        ConversationQuickReplyActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            replyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            //noinspection deprecation
            replyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }
        replyIntent.putExtra(applicationContext.getString(
            R.string.conversation_extra_contact), contact);
        PendingIntent replyPendingIntent = PendingIntent.getActivity(
            applicationContext, 0, replyIntent,
            PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Action.Builder replyAction =
            new NotificationCompat.Action.Builder(
                R.drawable.ic_reply_white_24dp,
                applicationContext.getString(
                    R.string.notifications_button_reply),
                replyPendingIntent);
        notification.addAction(replyAction.build());

        Intent markAsReadIntent = new Intent(applicationContext,
                                             MarkAsReadReceiver.class);
        markAsReadIntent.putExtra(applicationContext.getString(
            R.string.conversation_extra_contact), contact);
        PendingIntent markAsReadPendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, markAsReadIntent,
            PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Action.Builder markAsReadAction =
            new NotificationCompat.Action.Builder(
                R.drawable.ic_drafts_white_24dp,
                applicationContext.getString(
                    R.string.notifications_button_mark_read),
                markAsReadPendingIntent);
        notification.addAction(markAsReadAction.build());

        int id;
        if (notificationIds.get(contact) != null) {
            id = notificationIds.get(contact);
        } else {
            id = notificationIdCount++;
            notificationIds.put(contact, id);
        }
        NotificationManager notificationManager = (NotificationManager)
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification.build());
    }

}
