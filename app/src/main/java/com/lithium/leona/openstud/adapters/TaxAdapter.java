package com.lithium.leona.openstud.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lithium.leona.openstud.R;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;

import lithium.openstud.driver.core.PaymentDescription;
import lithium.openstud.driver.core.Tax;

public class TaxAdapter extends RecyclerView.Adapter<TaxAdapter.TaxHolder> {

    public enum Mode {
        PAID(0), UNPAID(1);
        private final int value;

        private Mode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private List<Tax> taxes;
    private Context context;
    private int mode;

    public TaxAdapter(Context context, List<Tax> taxes, int mode) {
        this.taxes = taxes;
        this.context = context;
        this.mode = mode;
    }

    @NonNull
    @Override
    public TaxHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_row_tax,parent, false);
        TaxHolder holder = new TaxHolder(view);
        holder.setMode(mode);
        holder.setContext(context);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull TaxHolder holder, int position) {
        Tax tax = taxes.get(position);
        holder.setDetails(tax);
    }

    @Override
    public int getItemCount() {
        return taxes.size();
    }

    public static class TaxHolder extends RecyclerView.ViewHolder  {
        private TextView txtCode,txtCourse,txtPaymentDescription,txtAcademicYear,txtPaymentAmount,txtPaymentDate;
        private int mode;
        private Context context;
        private void setMode(int mode){
            this.mode = mode;
        }

        private void setContext(Context context){
            this.context = context;
        }

        public TaxHolder(View itemView) {
            super(itemView);
            txtCode = itemView.findViewById(R.id.codeTax);
            txtCourse = itemView.findViewById(R.id.codeCourse);
            txtPaymentDescription = itemView.findViewById(R.id.mainDescription);
            txtAcademicYear = itemView.findViewById(R.id.academicYear);
            txtPaymentAmount = itemView.findViewById(R.id.payment_amount);
            txtPaymentDate = itemView.findViewById(R.id.paymentDate);
            }

        public void setDetails(Tax tax) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu");
            txtCourse.setText(context.getResources().getString(R.string.course_code_pay)+": "+tax.getCodeCourse());
            txtAcademicYear.setText(context.getResources().getString(R.string.accademic_year_pay)+": "+String.valueOf(tax.getAcademicYear()));
            txtPaymentAmount.setText(String.valueOf(tax.getAmount()+"€"));
            if (mode == Mode.UNPAID.getValue()) {
                txtCode.setText(context.getResources().getString(R.string.payment_number)+" "+tax.getCode());
                txtCode.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.big_text_tax));
                txtPaymentDescription.setVisibility(View.GONE);
                txtPaymentDate.setVisibility(View.GONE);
                txtPaymentDescription.setVisibility(View.GONE);
            }
            else {
                txtCode.setText(context.getResources().getString(R.string.payment_number_ext)+": "+tax.getCode());
                txtPaymentDescription.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.big_text_tax));
                txtPaymentDate.setText(context.getResources().getString(R.string.payment_date)+": "+tax.getPaymentDate().format(formatter).toString());
                List<PaymentDescription> list = tax.getPaymentDescriptionList();
                if (!list.isEmpty()) txtPaymentDescription.setText(tax.getPaymentDescriptionList().get(0).getDescription());
            }
        }
    }
}
