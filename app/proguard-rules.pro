-keepattributes Signature,*Annotation*

-keep class * extends android.support.v4.app.Fragment {
    public <init>();
}

-keep class com.grarak.kerneladiutor.views.NavHeaderView {
    public <init>(...);
}

-keep class com.grarak.kerneladiutor.views.XYGraph {
    public <init>(...);
}

-keep class com.viewpagerindicator.CirclePageIndicator {
    public <init>(...);
}

-keep class org.adw.library.widgets.discreteseekbar.DiscreteSeekBar {
    public <init>(...);
}
