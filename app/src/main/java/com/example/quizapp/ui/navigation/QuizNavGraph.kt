package com.example.quizapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.quizapp.ui.screens.banks.BankDetailScreen
import com.example.quizapp.ui.screens.banks.BankEditScreen
import com.example.quizapp.ui.screens.banks.BankListScreen
import com.example.quizapp.ui.screens.banks.QuestionEditScreen
import com.example.quizapp.ui.screens.practice.PracticeHomeScreen
import com.example.quizapp.ui.screens.practice.QuizResultScreen
import com.example.quizapp.ui.screens.practice.QuizScreen
import com.example.quizapp.ui.screens.records.RecordsScreen
import com.example.quizapp.ui.viewmodels.QuizViewModel

@Composable
fun QuizNavGraph(
    navController: NavHostController,
    qvm: QuizViewModel
) {
    NavHost(navController = navController, startDestination = Routes.Banks.route) {

        composable(Routes.Banks.route) {
            BankListScreen(
                onOpenBank = { id -> navController.navigate(Routes.BankDetail.create(id)) },
                onEditBank = { id -> navController.navigate(Routes.BankEdit.create(id)) }
            )
        }

        composable(
            Routes.BankDetail.route,
            arguments = listOf(navArgument("bankId") { type = NavType.LongType })
        ) { entry ->
            val bankId = entry.arguments?.getLong("bankId") ?: 0L
            BankDetailScreen(
                bankId = bankId,
                onBack = { navController.popBackStack() },
                onEditBank = { navController.navigate(Routes.BankEdit.create(bankId)) },
                onEditQuestion = { qid -> navController.navigate(Routes.QuestionEdit.create(bankId, qid)) },
                onStartPractice = {
                    qvm.startPractice(bankId) { ok -> if (ok) navController.navigate(Routes.Quiz.route) }
                },
                onStartRandomExam = {
                    qvm.startRandomExam(bankId, 10, true) { ok -> if (ok) navController.navigate(Routes.Quiz.route) }
                }
            )
        }

        composable(
            Routes.BankEdit.route,
            arguments = listOf(navArgument("bankId") { type = NavType.LongType })
        ) { entry ->
            val bankId = entry.arguments?.getLong("bankId") ?: 0L
            BankEditScreen(
                bankId = bankId,
                onBack = { navController.popBackStack() },
                onEditQuestion = { qid -> navController.navigate(Routes.QuestionEdit.create(bankId, qid)) }
            )
        }

        composable(
            Routes.QuestionEdit.route,
            arguments = listOf(
                navArgument("bankId") { type = NavType.LongType },
                navArgument("questionId") { type = NavType.LongType }
            )
        ) { entry ->
            val bankId = entry.arguments?.getLong("bankId") ?: 0L
            val qid = entry.arguments?.getLong("questionId") ?: 0L
            QuestionEditScreen(
                bankId = bankId,
                questionId = qid,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Practice.route) {
            PracticeHomeScreen(
                onSessionReady = { navController.navigate(Routes.Quiz.route) },
                vm = qvm
            )
        }

        composable(Routes.Quiz.route) {
            QuizScreen(
                onExit = { navController.popBackStack() },
                onFinished = { navController.navigate(Routes.QuizResult.route) },
                vm = qvm
            )
        }

        composable(Routes.QuizResult.route) {
            QuizResultScreen(
                onHome = { navController.popBackStack(Routes.Banks.route, inclusive = false) },
                vm = qvm
            )
        }

        composable(Routes.Records.route) {
            RecordsScreen(
                onPracticeReady = { navController.navigate(Routes.Quiz.route) },
                qvm = qvm
            )
        }
    }
}
