package com.example.aplication.ui.jobs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplication.R;
import com.example.aplication.adapters.JobAdapter;
import com.example.aplication.databinding.FragmentJobsBinding;
import com.example.aplication.models.Application;
import com.example.aplication.models.Job;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JobsFragment extends Fragment {

    private FragmentJobsBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private RecyclerView recyclerView;
    private JobAdapter jobAdapter;
    private List<Job> jobList;
    private String userRole;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentJobsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        jobList = new ArrayList<>();
        recyclerView = binding.recyclerView;
        progressBar = binding.progressBar;
        tvEmpty = binding.tvEmpty;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        jobAdapter = new JobAdapter(getContext(), jobList, "");
        recyclerView.setAdapter(jobAdapter);

        String userEmail = auth.getCurrentUser().getEmail();

        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            userRole = document.getString("rol");
                            jobAdapter = new JobAdapter(getContext(), jobList, userRole);
                            recyclerView.setAdapter(jobAdapter);

                            if (Objects.equals(userRole, "Empresa")) {
                                binding.btnCrear.setVisibility(View.VISIBLE);
                                binding.btnCrear.setOnClickListener(v -> {
                                    NavController navController = Navigation.findNavController(v);
                                    navController.navigate(R.id.action_nav_jobs_to_create_job);
                                });
                                loadCompanyJobs(userEmail);
                            } else if (Objects.equals(userRole, "Administrador")) {
                                loadAllJobs();
                            } else {
                                loadJobs(userEmail);
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "No se encontró el rol para el email especificado", Toast.LENGTH_SHORT).show();
                    }
                });

        return root;
    }

    private void loadCompanyJobs(String email) {
        db.collection("jobs")
                .whereEqualTo("companyEmail", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        jobList.clear();
                        if (!task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Job job = document.toObject(Job.class);
                                jobList.add(job);
                            }
                            progressBar.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                        jobAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Error al obtener trabajos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadJobs(String email) {
        List<String> appliedJobIds = new ArrayList<>();

        db.collection("applications")
                .whereEqualTo("workerEmail", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Application application = document.toObject(Application.class);
                            appliedJobIds.add(application.getJobId());
                        }

                        db.collection("jobs")
                                .get()
                                .addOnCompleteListener(jobTask -> {
                                    if (jobTask.isSuccessful() && !jobTask.getResult().isEmpty()) {
                                        jobList.clear();
                                        if (!jobTask.getResult().isEmpty()) {
                                            for (QueryDocumentSnapshot jobDocument : jobTask.getResult()) {
                                                Job job = jobDocument.toObject(Job.class);
                                                if (!appliedJobIds.contains(job.getJobId()) && job.getVacancies() > 0) {
                                                    jobList.add(job);
                                                }
                                            }
                                            if (jobList.isEmpty()) {
                                                tvEmpty.setVisibility(View.VISIBLE);
                                            } else {
                                                tvEmpty.setVisibility(View.GONE);
                                            }
                                            progressBar.setVisibility(View.GONE);
                                            recyclerView.setVisibility(View.VISIBLE);
                                        } else {
                                            progressBar.setVisibility(View.GONE);
                                            tvEmpty.setVisibility(View.VISIBLE);
                                        }
                                        jobAdapter.notifyDataSetChanged();
                                    } else {
                                        Toast.makeText(getContext(), "Error al obtener trabajos", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Error al obtener postulaciones", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAllJobs() {
        db.collection("jobs")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        jobList.clear();
                        if (!task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Job job = document.toObject(Job.class);
                                jobList.add(job);
                            }
                            if (jobList.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                            }
                            progressBar.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                        jobAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Error al obtener trabajos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}