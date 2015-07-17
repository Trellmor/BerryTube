/*
 * BerryTubeChat android client
 * Copyright (C) 2012-2013 Daniel Triendl <trellmor@trellmor.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.trellmor.berrytubechat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.TextView;

import com.trellmor.berrymotes.EmoteGetter;
import com.trellmor.berrymotes.EmotesFormatter;
import com.trellmor.berrymotes.loader.ScalingEmoteLoader;
import com.trellmor.berrytube.ChatMessage;
import com.trellmor.berrytube.ChatUser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class is used to inflate and populate a <code>View</code> to display a
 * <code>ChatMessage</code>
 *
 * @author Daniel Triendl
 * @see com.trellmor.berrytube.ChatMessage
 */
class ChatMessageFormatter {
	public static final int TYPE_DEFAULT = 0;
	public static final int TYPE_DRINK = 1;
	private final EmotesFormatter mEmotesFormatter;
	private Context mContext = null;
	private LayoutInflater mInflater = null;
	private Html.ImageGetter mEmoteGetter = null;
	private final Drawable[] mFlairs = new Drawable[6];
	private String mNick = null;

	public ChatMessageFormatter(Context context) {
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mEmoteGetter = new EmoteGetter(mContext, new ScalingEmoteLoader(mContext));
		mEmotesFormatter = new EmotesFormatter(mContext);

		// Initialize flair drawables
		loadFlairs();
	}

	public View inflate(ChatMessage message) {
		switch (message.getEmote()) {
			case ChatMessage.EMOTE_DRINK:
				return inflateDrink();
			default:
				return inflateDefault();
		}
	}

	public void format(View view, ChatMessage message) {
		switch (message.getEmote()) {
			case ChatMessage.EMOTE_DRINK:
				formatDrinks(view, message);
				break;
			default:
				formatDefault(view, message);
				break;
		}
	}

	public int getViewType(ChatMessage message) {
		if (containsBerryMotes(message.getMsg()))
			return Adapter.IGNORE_ITEM_VIEW_TYPE;

		switch (message.getEmote()) {
			case ChatMessage.EMOTE_DRINK:
				return TYPE_DRINK;
			default:
				return TYPE_DEFAULT;
		}
	}

	public void setNick(String nick) {
		mNick = nick;
	}

	private View inflateDefault() {
		View view = mInflater.inflate(R.layout.chat_item, null);
		view.setTag(R.id.text_chat_message, view.findViewById(R.id.text_chat_message));
		return view;
	}

	private View inflateDrink() {
		View view = mInflater.inflate(R.layout.chat_item_drink, null);
		view.setTag(R.id.text_chat_message, view.findViewById(R.id.text_chat_message));
		view.setTag(R.id.text_chat_drink_multiple, view.findViewById(R.id.text_chat_drink_multiple));
		return view;
	}

	private View formatDrinks(View view, ChatMessage message) {
		if (view == null || view.getId() != R.id.chat_item_drink) {
			throw new IllegalArgumentException("view has to be chat_item_drink");
		}

		TextView textMessage = (TextView) view.getTag(R.id.text_chat_message);
		TextView textMulti = (TextView) view.getTag(R.id.text_chat_drink_multiple);

		SpannableStringBuilder spanBuilder = new SpannableStringBuilder();

		if (message.getEmote() != ChatMessage.EMOTE_POLL) {
			spanBuilder.append(createTimestamp(message.getTimestamp()));
		}
		int endTime = spanBuilder.length();
		handleNick(spanBuilder, message);
		spanBuilder.append(": ");
		handleMessage(spanBuilder, message);
		spanBuilder.append(' ').append(mContext.getString(R.string.chat_drink));
		spanBuilder.setSpan(new RelativeSizeSpan(1.2f), endTime, spanBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		textMessage.setText(spanBuilder);

		if (message.getMulti() > 1) {
			textMulti.setText(Integer.toString(message.getMulti()) + "x");
			textMulti.setVisibility(View.VISIBLE);
		} else {
			textMulti.setVisibility(View.GONE);
		}

		return view;
	}

	private void formatDefault(View view, ChatMessage message) {
		if (view == null || view.getId() != R.id.text_chat_message) {
			throw new IllegalArgumentException("view has to be chat_item");
		}

		TextView textMessage = (TextView) view.getTag(R.id.text_chat_message);
		textMessage.setText(formatChatMsg(message));
	}

	private String createTimestamp(long timeStamp) {
		String result = "";
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if (prefs.getBoolean(MainActivity.KEY_TIMESTAMP, false) && timeStamp > 0) {
			SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ", Locale.ENGLISH);
			result = sdf.format(new Date(timeStamp));
		}

		return result;
	}

	private void handleNick(SpannableStringBuilder text, ChatMessage message) {
		int len = message.getNick().length();
		int start = text.length();
		int end = start + len;
		text.append(message.getNick());
		text.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		if (message.isFlaunt()) {
			switch (message.getType()) {
				case ChatUser.TYPE_ADMIN:
					text.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.flaunt_admin)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					break;
				case ChatUser.TYPE_ASSISTANT:
					text.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.flaunt_assistant)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					break;
			}
		}
	}

	private void loadFlairs() {
		Resources r = mContext.getResources();
		mFlairs[0] = r.getDrawable(R.drawable.ic_flair_1);
		mFlairs[1] = r.getDrawable(R.drawable.ic_flair_2);
		mFlairs[2] = r.getDrawable(R.drawable.ic_flair_3);
		mFlairs[3] = r.getDrawable(R.drawable.ic_flair_4);
		mFlairs[4] = r.getDrawable(R.drawable.ic_flair_5);
		mFlairs[5] = r.getDrawable(R.drawable.ic_flair_6);

		for (Drawable d : mFlairs) {
			d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		}
	}

	private void handleFlair(SpannableStringBuilder text, ChatMessage message) {
		if (message.getFlair() > 0 && message.getFlair() <= mFlairs.length) {
			Drawable d = mFlairs[message.getFlair() - 1];
			if (d != null) {
				int len = text.length();
				text.append("\uFFFC");
				text.setSpan(new ImageSpan(d), len, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}

	public void handleMessage(SpannableStringBuilder text, ChatMessage message) {
		try {
			XMLReader parser = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
			ChatMessageHandler handler = new ChatMessageHandler(text, mEmoteGetter);
			if (message.isHighlightable()) {
				handler.addHighlight(mNick);
			}
			handler.setSpoiler(message.isHidden());
			parser.setContentHandler(handler);
			parser.parse(new InputSource(new StringReader(formatBerryMotes(message.getMsg()))));
		} catch (SAXException e) {
		} catch (IOException e) {
		}
	}

	private Spanned formatChatMsg(ChatMessage message) {
		SpannableStringBuilder spanBuilder = new SpannableStringBuilder();

		if (message.getEmote() != ChatMessage.EMOTE_POLL) {
			spanBuilder.append(createTimestamp(message.getTimestamp()));
		}
		int endTime = spanBuilder.length();
		handleNick(spanBuilder, message);
		handleFlair(spanBuilder, message);

		switch (message.getEmote()) {
			case ChatMessage.EMOTE_REQUEST:
				spanBuilder.append(' ').append(mContext.getText(R.string.request)).append(' ');
				break;
			case ChatMessage.EMOTE_SPOILER:
				spanBuilder.append(": SPOILER: ");
				break;
			case ChatMessage.EMOTE_ACT:
				spanBuilder.append(" ");
				break;
			default:
				spanBuilder.append(": ");
				break;
		}

		int start = spanBuilder.length();
		handleMessage(spanBuilder, message);
		int len = spanBuilder.length();

		// SWEETIEBOT
		int c;
		switch (message.getEmote()) {
			case ChatMessage.EMOTE_SWEETIEBOT:
				spanBuilder.setSpan(new TypefaceSpan("courier new"), start, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				break;
			case ChatMessage.EMOTE_ACT:
				spanBuilder.setSpan(new ForegroundColorSpan(Color.GRAY), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				spanBuilder.setSpan(new StyleSpan(Typeface.ITALIC), endTime, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				break;
			case ChatMessage.EMOTE_REQUEST:
				spanBuilder.setSpan(new ForegroundColorSpan(Color.BLUE), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				spanBuilder.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), endTime, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				break;
			case ChatMessage.EMOTE_RCV:
				spanBuilder.setSpan(new ForegroundColorSpan(Color.RED), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				spanBuilder.setSpan(new RelativeSizeSpan(1.5f), endTime, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				break;
			case ChatMessage.EMOTE_POLL:
				c = mContext.getResources().getColor(R.color.poll);
				spanBuilder.setSpan(new ForegroundColorSpan(c), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				spanBuilder.setSpan(new RelativeSizeSpan(1.5f), endTime, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				spanBuilder.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				spanBuilder.setSpan(new StyleSpan(Typeface.BOLD), endTime, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				break;
			default:
				// implying
				if (message.getMsg().startsWith("&gt;")) {
					c = mContext.getResources().getColor(R.color.implying);
					spanBuilder.setSpan(new ForegroundColorSpan(c), start, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				break;
		}

		// Spoiler coloring
		// Remove all ForegroundColorSpans that occur during the message
		if (message.isHidden()) {
			Object[] spans = spanBuilder.getSpans(start, spanBuilder.length(), ForegroundColorSpan.class);
			for (Object span : spans) {
				spanBuilder.removeSpan(span);
			}

			// If it's a spoiler emote, set the whole background to black
			if (message.getEmote() == ChatMessage.EMOTE_SPOILER) {
				spanBuilder.setSpan(new BackgroundColorSpan(Color.BLACK), start, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}

		return spanBuilder;
	}

	private String formatBerryMotes(String msg) {
		return mEmotesFormatter.formatString(msg, "emote");
	}

	private boolean containsBerryMotes(String msg) {
		return mEmotesFormatter.containsEmotes(msg);
	}

	private static class Span {
		public String mClass;
		public String mStyle;

		public Span(String spanClass, String spanStyle) {
			mClass = spanClass;
			mStyle = spanStyle;
		}
	}

	private static class Bold {
	}

	private static class Strike {
	}

	private static class Italic {
	}

	class ChatMessageHandler extends DefaultHandler {
		private final SpannableStringBuilder mSpanBuilder;
		private final Html.ImageGetter mImageGetter;
		private final List<String> mHighlights = new ArrayList<>();
		private boolean mSpoiler = false;

		public ChatMessageHandler(SpannableStringBuilder spanBuilder, Html.ImageGetter imageGetter) {
			mSpanBuilder = spanBuilder;
			mImageGetter = imageGetter;
		}

		public void addHighlight(String highlight) {
			if (highlight != null) {
				mHighlights.add(highlight);
			}
		}

		public void setSpoiler(boolean spoiler) {
			mSpoiler = spoiler;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			if (localName.equalsIgnoreCase("span")) {
				startSpan(attributes);
			} else if (localName.equalsIgnoreCase("pre")) {
				startSpan(attributes);
			} else if (localName.equalsIgnoreCase("strong")) {
				start(new Bold());
			} else if (localName.equalsIgnoreCase("b")) {
				start(new Bold());
			} else if (localName.equalsIgnoreCase("strike")) {
				start(new Strike());
			} else if (localName.equalsIgnoreCase("i")) {
				start(new Italic());
			} else if (localName.equalsIgnoreCase("em")) {
				start(new Italic());
			} else if (localName.equalsIgnoreCase("emote")) {
				startEmote(attributes);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (localName.equalsIgnoreCase("span")) {
				endSpan();
			} else if (localName.equalsIgnoreCase("pre")) {
				endSpan();
			} else if (localName.equalsIgnoreCase("strong")) {
				end(Bold.class, new StyleSpan(Typeface.BOLD));
			} else if (localName.equalsIgnoreCase("b")) {
				end(Bold.class, new StyleSpan(Typeface.BOLD));
			} else if (localName.equalsIgnoreCase("strike")) {
				end(Strike.class, new StrikethroughSpan());
			} else if (localName.equalsIgnoreCase("i")) {
				end(Italic.class, new StyleSpan(Typeface.ITALIC));
			} else if (localName.equalsIgnoreCase("em")) {
				end(Italic.class, new StyleSpan(Typeface.ITALIC));
			}
		}

		private Object getLast(Class kind) {
			Object[] objs = mSpanBuilder.getSpans(0, mSpanBuilder.length(), kind);

			if (objs.length == 0) {
				return null;
			} else {
				return objs[objs.length - 1];
			}
		}

		private void start(Object mark) {
			int len = mSpanBuilder.length();
			mSpanBuilder.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
		}

		private void startSpan(Attributes attributes) {
			String spanClass = attributes.getValue("", "class");
			String spanStyle = attributes.getValue("", "style");

			int len = mSpanBuilder.length();
			mSpanBuilder.setSpan(new Span(spanClass, spanStyle), len, len, Spannable.SPAN_MARK_MARK);
		}

		private void startEmote(Attributes attributes) {
			String src = attributes.getValue("", "src");
			Drawable d = mImageGetter.getDrawable(src);

			if (d != null) {
				int len = mSpanBuilder.length();
				mSpanBuilder.append("\uFFFC");

				mSpanBuilder.setSpan(new ImageSpan(d, src), len, mSpanBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else {
				if (src.indexOf('/') < 0) {
					mSpanBuilder.append("[](/").append(src).append(')');
				}
			}
		}

		private void end(Class kind, Object repl) {
			int len = mSpanBuilder.length();
			Object obj = getLast(kind);
			int where = mSpanBuilder.getSpanStart(obj);

			mSpanBuilder.removeSpan(obj);

			if (where != len) {
				mSpanBuilder.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}

		private void endSpan() {
			int len = mSpanBuilder.length();
			Object obj = getLast(Span.class);
			int where = mSpanBuilder.getSpanStart(obj);

			mSpanBuilder.removeSpan(obj);

			if (where != len) {
				Span s = (Span) obj;

				if (s.mClass != null) {
					if (s.mClass.equals("flutter")) {
						mSpanBuilder.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.flutter)), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					} else if (mSpoiler && s.mClass.equals("spoiler")) {
						mSpanBuilder.setSpan(new BackgroundColorSpan(Color.BLACK), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}

				if (s.mStyle != null) {
					String[] styles = s.mStyle.split(";");
					for (String style : styles) {
						String[] styleDec = style.split(":");
						if (styleDec.length == 2) {
							String name = styleDec[0].trim();
							if (name.equalsIgnoreCase("color")) {
								try {
									int c = Color.parseColor(styleDec[1].trim());
									mSpanBuilder.setSpan(new ForegroundColorSpan(c), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								} catch (IllegalArgumentException e) {
								}
							} else if (name.equalsIgnoreCase("font-weight")) {
								if (styleDec[1].equalsIgnoreCase("bold")) {
									mSpanBuilder.setSpan(new StyleSpan(Typeface.BOLD), where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
								}
							} else if (name.equalsIgnoreCase("font-family")) {
								mSpanBuilder.setSpan(new TypefaceSpan(styleDec[1]), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							}
						}
					}
				}
			}
		}

		public void characters(char ch[], int start, int length) throws SAXException {
			String text = new String(ch, start, length);

			int len = mSpanBuilder.length();
			mSpanBuilder.append(text);

			for (String highlight : mHighlights) {
				int p = 0;
				int hlLen = highlight.length();
				while ((p = text.indexOf(highlight, p)) >= 0) {
					mSpanBuilder.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.highlight)),
							len + p, len + p + hlLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					p = p + hlLen;
				}
			}
		}
	}
}
