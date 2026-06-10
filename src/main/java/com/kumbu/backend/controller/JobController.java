package com.kumbu.backend.controller;



import com.kumbu.backend.dto.job.CvCreateRequest;

import com.kumbu.backend.dto.job.JobApplyRequest;

import com.kumbu.backend.dto.job.JobRespondRequest;

import com.kumbu.backend.service.JobService;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.List;

import java.util.UUID;



@RestController

@RequestMapping("/api/v1/jobs")

@RequiredArgsConstructor

@Validated

public class JobController {



    private final JobService jobService;



    @GetMapping

    public List<com.kumbu.backend.dto.catalog.ListingResponse> listJobs(

            @RequestParam(required = false) @Size(max = 120) String q,

            @RequestParam(required = false) @Size(max = 64) String province,

            @RequestParam(required = false) @Size(max = 64) String municipality,

            @RequestParam(required = false) @Size(max = 40) String contractType,

            @RequestParam(required = false) @Size(max = 80) String sector,

            @RequestParam(required = false) Boolean remote) {

        return jobService.listActiveJobs(q, province, municipality, contractType, sector, remote);

    }



    @GetMapping("/cvs")

    public List<JobService.CvDto> listCvs() {

        return jobService.listMyCvs();

    }



    @PostMapping("/cvs")

    @ResponseStatus(HttpStatus.CREATED)

    public JobService.CvDto createCv(@Valid @RequestBody CvCreateRequest request) {

        return jobService.createCv(request);

    }



    @DeleteMapping("/cvs/{id}")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void deleteCv(@PathVariable UUID id) {

        jobService.deleteCv(id);

    }



    @PatchMapping("/cvs/{id}")

    public JobService.CvDto updateCv(@PathVariable UUID id, @Valid @RequestBody CvCreateRequest request) {

        return jobService.updateCv(id, request);

    }



    @PostMapping("/{jobId}/apply")

    @ResponseStatus(HttpStatus.CREATED)

    public JobService.ApplicationDto apply(

            @PathVariable @NotBlank @Size(max = 64) String jobId,

            @Valid @RequestBody JobApplyRequest request) {

        return jobService.apply(jobId, request.getCvId(), request.getCoverMessage());

    }



    @GetMapping("/applications/mine")

    public List<JobService.ApplicationDto> myApplications() {

        return jobService.listMyApplications();

    }



    @GetMapping("/applications/employer")

    public List<JobService.ApplicationDto> employerApplications(

            @RequestParam(required = false) @Size(max = 40) String status,

            @RequestParam(required = false) @Size(max = 120) String q,

            @RequestParam(required = false) @Size(max = 64) String province) {

        return jobService.listEmployerApplications(status, q, province);

    }



    @PostMapping("/applications/{id}/respond")

    public JobService.ApplicationDto respond(@PathVariable UUID id, @Valid @RequestBody JobRespondRequest request) {

        return jobService.respond(id, request.getAction());

    }



    @PostMapping("/applications/{id}/cv-view")

    public JobService.CvViewResponse recordCvView(@PathVariable UUID id) {

        return jobService.recordCvView(id);

    }



    @PostMapping("/{jobId}/filled")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void markFilled(@PathVariable @NotBlank @Size(max = 64) String jobId) {

        jobService.markJobFilled(jobId);

    }

}

