package com.sanangames.tapomaly;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public final class AccountSheet {

    public interface Host {
        void playClick();

        void onClosed();
    }

    private AccountSheet() {
    }

    public static AlertDialog show(Activity activity, Host host) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        if (Session.isSignedIn(activity)) {
            return showSignedIn(activity, host);
        }
        return showGuest(activity, host);
    }

    private static AlertDialog showGuest(Activity activity, Host host) {
        AlertDialog dialog = new AlertDialog.Builder(activity).create();
        boolean[] creating = {false};

        LinearLayout content = column(activity);
        TextView title = title(activity, "Account");
        TextView subtitle = body(activity, "Play without an account. Sign in only to save progress and keep pack purchases.");
        content.addView(title);
        content.addView(subtitle);

        EditText email = field(activity, "Email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password = field(activity, "Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText nickname = field(activity, "Nickname", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        nickname.setVisibility(View.GONE);
        content.addView(email);
        content.addView(gap(activity, 10));
        content.addView(password);
        content.addView(gap(activity, 10));
        content.addView(nickname);

        LinearLayout modes = new LinearLayout(activity);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        TextView signInMode = modeButton(activity, "Sign in", true);
        TextView createMode = modeButton(activity, "Create account", false);
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(0, dp(activity, 40), 1);
        modeParams.setMarginEnd(dp(activity, 8));
        modes.addView(signInMode, modeParams);
        modes.addView(createMode, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
        LinearLayout.LayoutParams modesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        modesParams.topMargin = dp(activity, 14);
        content.addView(modes, modesParams);

        TextView submit = action(activity, "Sign in", true);
        TextView close = action(activity, "Close", false);
        content.addView(submit, buttonParams(activity, 16));
        content.addView(close, buttonParams(activity, 8));

        signInMode.setOnClickListener(v -> {
            click(host);
            creating[0] = false;
            nickname.setVisibility(View.GONE);
            submit.setText("Sign in");
            paintMode(activity, signInMode, true);
            paintMode(activity, createMode, false);
        });
        createMode.setOnClickListener(v -> {
            click(host);
            creating[0] = true;
            nickname.setVisibility(View.VISIBLE);
            submit.setText("Create account");
            paintMode(activity, signInMode, false);
            paintMode(activity, createMode, true);
        });
        submit.setOnClickListener(v -> {
            click(host);
            String emailText = text(email);
            String passwordText = text(password);
            String nicknameText = text(nickname);
            String error = validate(creating[0], emailText, passwordText, nicknameText);
            if (error != null) {
                Toast.makeText(activity, error, Toast.LENGTH_SHORT).show();
                return;
            }
            submit.setEnabled(false);
            StoreApi.Result result = new StoreApi.Result() {
                @Override
                public void onSuccess(Object data) {
                    if (data instanceof JSONObject) {
                        Session.saveTokens(activity, (JSONObject) data);
                    }
                    Session.saveProfile(activity, emailText, creating[0] ? nicknameText : "");
                    ProgressSync.pull(activity);
                    Toast.makeText(activity, creating[0] ? "Account created" : "Signed in", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String message) {
                    submit.setEnabled(true);
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                }
            };
            if (creating[0]) {
                StoreApi.register(activity, emailText, passwordText, nicknameText, result);
            } else {
                StoreApi.login(activity, emailText, passwordText, result);
            }
        });
        close.setOnClickListener(v -> {
            click(host);
            dialog.dismiss();
        });

        present(activity, dialog, content, host);
        return dialog;
    }

    private static AlertDialog showSignedIn(Activity activity, Host host) {
        AlertDialog dialog = new AlertDialog.Builder(activity).create();
        LinearLayout content = column(activity);
        content.addView(title(activity, Session.nickname(activity)));
        content.addView(body(activity, Session.email(activity)));
        content.addView(body(
                activity,
                "Level " + Session.unlockedLevel(activity) + "  ·  Best " + Session.bestScore(activity)
        ));

        TextView save = action(activity, "Save progress", true);
        TextView signOut = action(activity, "Sign out", false);
        TextView close = action(activity, "Close", false);
        content.addView(save, buttonParams(activity, 16));
        content.addView(signOut, buttonParams(activity, 8));
        content.addView(close, buttonParams(activity, 8));

        save.setOnClickListener(v -> {
            click(host);
            save.setEnabled(false);
            ProgressSync.save(activity, true, saved -> save.setEnabled(true));
        });
        signOut.setOnClickListener(v -> {
            click(host);
            signOut.setEnabled(false);
            StoreApi.logout(activity, new StoreApi.Result() {
                @Override
                public void onSuccess(Object data) {
                    Session.clear(activity);
                    dialog.dismiss();
                }

                @Override
                public void onError(String message) {
                    Session.clear(activity);
                    dialog.dismiss();
                }
            });
        });
        close.setOnClickListener(v -> {
            click(host);
            dialog.dismiss();
        });

        present(activity, dialog, content, host);
        return dialog;
    }

    private static String validate(boolean creating, String email, String password, String nickname) {
        if (!email.contains("@") || email.startsWith("@") || email.endsWith("@")) {
            return "Enter a valid email";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        if (creating && nickname.isEmpty()) {
            return "Enter a nickname";
        }
        return null;
    }

    private static void present(Activity activity, AlertDialog dialog, View content, Host host) {
        LinearLayout shell = new LinearLayout(activity);
        shell.setBackground(dialogBackground(activity));
        shell.addView(content);
        dialog.setView(shell);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(d -> {
            if (host != null) {
                host.onClosed();
            }
        });
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window == null) {
                return;
            }
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 36);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setDimAmount(0.72f);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        });
        dialog.show();
    }

    private static LinearLayout column(Activity activity) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 20), dp(activity, 18), dp(activity, 20), dp(activity, 16));
        return content;
    }

    private static TextView title(Activity activity, String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(26);
        view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private static TextView body(Activity activity, String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(Color.parseColor("#D8E2F3"));
        view.setTextSize(14);
        view.setPadding(0, dp(activity, 6), 0, 0);
        return view;
    }

    private static EditText field(Activity activity, String hint, int inputType) {
        EditText field = new EditText(activity);
        field.setHint(hint);
        field.setInputType(inputType);
        field.setSingleLine(true);
        field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.parseColor("#8EA0C2"));
        field.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#0E1730"));
        background.setCornerRadius(dp(activity, 14));
        background.setStroke(dp(activity, 1), Color.parseColor("#6D7EA8"));
        field.setBackground(background);
        return field;
    }

    private static TextView modeButton(Activity activity, String text, boolean selected) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(14);
        view.setTypeface(null, Typeface.BOLD);
        paintMode(activity, view, selected);
        return view;
    }

    private static void paintMode(Activity activity, TextView view, boolean selected) {
        view.setTextColor(selected ? Color.parseColor("#062033") : Color.WHITE);
        view.setBackground(selected ? primary(activity) : secondary(activity));
    }

    private static TextView action(Activity activity, String text, boolean primary) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(16);
        view.setTypeface(null, Typeface.BOLD);
        view.setTextColor(primary ? Color.parseColor("#062033") : Color.WHITE);
        view.setBackground(primary ? primary(activity) : secondary(activity));
        return view;
    }

    private static LinearLayout.LayoutParams buttonParams(Activity activity, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 46)
        );
        params.topMargin = dp(activity, top);
        return params;
    }

    private static View gap(Activity activity, int value) {
        View view = new View(activity);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, value)
        ));
        return view;
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private static void click(Host host) {
        if (host != null) {
            host.playClick();
        }
    }

    private static GradientDrawable dialogBackground(Activity activity) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.parseColor("#20113F"),
                        Color.parseColor("#121A33"),
                        Color.parseColor("#061B3A")
                }
        );
        drawable.setCornerRadius(dp(activity, 26));
        drawable.setStroke(dp(activity, 2), Color.parseColor("#8B5CF6"));
        return drawable;
    }

    private static GradientDrawable primary(Activity activity) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.parseColor("#06D6A0"),
                        Color.parseColor("#00B4D8")
                }
        );
        drawable.setCornerRadius(dp(activity, 14));
        return drawable;
    }

    private static GradientDrawable secondary(Activity activity) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#26324A"));
        drawable.setCornerRadius(dp(activity, 14));
        drawable.setStroke(dp(activity, 1), Color.parseColor("#6D7EA8"));
        return drawable;
    }

    private static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density);
    }
}
