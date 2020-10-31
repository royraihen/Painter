package com.example.painter;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;

import com.example.painter.Adapter.ColorAdapter;
import com.example.painter.Interface.RotateFragmentListener;
import com.example.painter.Interface.TextFragmentListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


public class RotateFragment extends BottomSheetDialogFragment implements ColorAdapter.ColorAdapterListener {

    SeekBar seekBar_rotate;

    RotateFragmentListener listener;

    static RotateFragment instace;

    public static RotateFragment getInstance(){
        if(instace == null){
            instace = new RotateFragment();
        }
        return instace;
    }

    public void setListener(RotateFragmentListener listener){
        this.listener = listener;
    }


    public RotateFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_rotate, container, false);

        seekBar_rotate = view.findViewById(R.id.seekbar_rotate);

        seekBar_rotate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                System.out.println(i);
                listener.onRotateAngleChangedListener(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        return view;
    }

    @Override
    public void onColorSelected(int color) {
        color=3;
    }
}