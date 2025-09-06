package com.example.famme.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping("/")
class PullRequest {


    @GetMapping("/pr")
    fun pr() : String {
        return "Pull Request"
    }

    @GetMapping("/newBranch")
    fun newBranch() : String {
        return "New Branch"
    }
}
