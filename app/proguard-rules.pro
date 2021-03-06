-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-libraryjars E:\framework_2_2_classes.jar


-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep class * extends android.view.View
-keep class * extends android.view.ViewGroup
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
-keep public class com.nd.weather.widget.Ctrl.MyCheckButton
-keep public class * extends android.support.v4.**
-keep public class * extends android.app.Fragment
-dontwarn android.support.v4.**

-keep class **.R$* {  
 *;  
}


-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    static android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-dontwarn com.nd.analytics.** 
 
-keep public class com.nd.analytics.** { 
  *; 
} 

-keep class android.support.v4.** {
	*;
}

-keep public class com.nd.weather.widget.PandaHome.PandaWidgetView {
	public *;
}
-keep public class * extends com.nd.weather.widget.PandaHome.PandaWidgetView {
	public *;
}

-keep public class com.calendar.CommData.DateInfo {
	public protected private *;
}

-keep public class com.calendar.CommData.LunarInfo {
	public protected private *;
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}