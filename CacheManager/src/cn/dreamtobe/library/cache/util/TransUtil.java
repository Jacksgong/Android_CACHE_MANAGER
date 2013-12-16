package cn.dreamtobe.library.cache.util;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * @describe 转换工具类
 * 
 * @author Jacksgong
 * @since 2013-12-16 下午8:24:40
 * @Web http://blog.dreamtobe.cn
 */
public class TransUtil {

	public static byte[] bitmap2Bytes(Bitmap bm) {
		if (bm == null) {
			return null;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}

	public static Bitmap bytes2Bimap(byte[] b) {
		if (b != null && b.length > 0) {
			return BitmapFactory.decodeByteArray(b, 0, b.length);
		}

		return null;
	}

	public static byte[] parcelable2Bytes(Parcelable p) {
		if (p == null) {
			return null;
		}
		Parcel parcel = Parcel.obtain();
		p.writeToParcel(parcel, 0);
		byte[] b = parcel.marshall();
		parcel.recycle();
		return b;
	}

	public static Parcel bytes2Parcelable(byte[] b) {
		if (b == null) {
			return null;
		}
		Parcel p = Parcel.obtain();
		p.unmarshall(b, 0, b.length);
		p.setDataPosition(0);
		return p;
	}
}