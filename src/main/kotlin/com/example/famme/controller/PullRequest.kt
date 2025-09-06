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

    @GetMapping("/secondBranch")
    fun secondBranch() : String {
        return "Second Branch"
    }

    @GetMapping("/secondBranch")
    fun thirdBranch() : String {
        return "Third Branch"
    }


    @GetMapping("/secondBranch")
    fun mergeBranch() : String {
        return "merge Branch"
    }

    @GetMapping("/secondBranch")
    fun rebaseBranch() : String {
        return "Rebase 1 Branch"
    }

    @GetMapping("/secondBranch")
    fun rebasBranch() : String {
        return "Rebase 2 Branch"
    }
}
