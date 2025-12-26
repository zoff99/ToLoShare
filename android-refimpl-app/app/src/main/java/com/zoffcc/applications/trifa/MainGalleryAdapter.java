/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2025 Zoff <zoff@zoff.cc>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package com.zoffcc.applications.trifa;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import info.guardianproject.iocipher.File;

import static com.zoffcc.applications.trifa.HelperFiletransfer.share_local_file;
import static com.zoffcc.applications.trifa.HelperFriend.get_set_is_default_ft_contact;
import static com.zoffcc.applications.trifa.ImageviewerActivity.current_image_postiton_in_list;
import static com.zoffcc.applications.trifa.MainActivity.SD_CARD_FILES_EXPORT_DIR;

public class MainGalleryAdapter extends RecyclerView.Adapter<MainGalleryAdapter.ViewHolder> {

    private static final String TAG = "trifa.GalleryAdapterr";

    private final Context context;
    public static ArrayList<String> maingallery_images_list = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.main_gallery_item,null,true);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final String filename_fullpath = maingallery_images_list.get(holder.getBindingAdapterPosition());
        holder.itemView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    Intent intent = new Intent(v.getContext(), ImageviewerActivity.class);
                    intent.putExtra("image_filename", filename_fullpath);
                    current_image_postiton_in_list = holder.getBindingAdapterPosition();
                    v.getContext().startActivity(intent);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(final View v)
            {
                try
                {
                    // HINT: just share the file
                    try
                    {
                        final String default_friend_pubkey = get_set_is_default_ft_contact(null, false);
                        if (default_friend_pubkey != null)
                        {
                            try_to_share_file_from_gallery(default_friend_pubkey, filename_fullpath, v.getContext());
                        }
                    }
                    catch (Exception ignored)
                    {
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                return true;
            }
        });

        File f2 = new File(filename_fullpath);
        try
        {
            // final RequestOptions glide_options = new RequestOptions().fitCenter().optionalTransform(
            //        new RoundedCorners((int) dp2px(200)));
            GlideApp.
                    with(context).
                    load(f2).
                    diskCacheStrategy(DiskCacheStrategy.RESOURCE).
                    skipMemoryCache(false).
                    priority(Priority.LOW).
                    placeholder(R.drawable.round_loading_animation).
                    into(holder.image);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return maingallery_images_list.size();
    }

    public MainGalleryAdapter(Context context, ArrayList<String> images_list) {
        this.context = context;
        maingallery_images_list = images_list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.gallery_item);
        }
    }

    public void try_to_share_file_from_gallery(final String default_friend_pubkey,
                                               final String vfs_filename_with_path, final Context c)
    {
        try
        {
            try
            {
                final String export_filename_dir =
                        SD_CARD_FILES_EXPORT_DIR + "/" + default_friend_pubkey +
                        "/";

                ProgressDialog progressDialog2 = null;
                try
                {
                    progressDialog2 = ProgressDialog.show(c, "",
                                                          "exporting File ...");
                    progressDialog2.setCanceledOnTouchOutside(false);
                    progressDialog2.setOnCancelListener(
                            new DialogInterface.OnCancelListener()
                            {
                                @Override
                                public void onCancel(DialogInterface dialog)
                                {
                                }
                            });
                }
                catch (Exception e3)
                {
                    e3.printStackTrace();
                }

                new save_selected_message_custom_asynchtask_from_gallery(c,
                                                                         progressDialog2,
                                                                         export_filename_dir,
                                                                         vfs_filename_with_path).execute();
            }
            catch (Exception e)
            {
            }
        }
        catch (Exception e)
        {
        }
    }

    static class save_selected_message_custom_asynchtask_from_gallery extends AsyncTask<Void, Void, String>
    {
        ProgressDialog progressDialog2;
        private final WeakReference<Context> weakContext;
        private final String export_directory;
        private final String fname;
        private final String vfs_filename_with_path;

        save_selected_message_custom_asynchtask_from_gallery(Context c, ProgressDialog progressDialog2,
                                                String export_filename_dir,
                                                String vfs_filename_with_path)
        {
            this.weakContext = new WeakReference<>(c);
            this.progressDialog2 = progressDialog2;
            this.export_directory = export_filename_dir;
            this.vfs_filename_with_path = vfs_filename_with_path;
            this.fname = new java.io.File(vfs_filename_with_path).getName();
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            try
            {
                // Log.i(TAG, "" + vfs_filename_with_path + " " + export_directory + " " + fname);
                HelperGeneric.export_vfs_file_to_real_file_2(vfs_filename_with_path, export_directory, fname);
            }
            catch (Exception e)
            {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            try
            {
                progressDialog2.dismiss();
                Context c = weakContext.get();
            }
            catch (Exception e4)
            {
                e4.printStackTrace();
            }

            share_local_file((export_directory + "/" + fname), weakContext.get());
        }

        @Override
        protected void onPreExecute()
        {
        }
    }
}
