package models.magicMerge

import components._
import scala.util.{Try, Success}
import models._
import models.PullRequest
import models.Branch
import scala.util.Failure
import exceptions.NotFoundException
import scala.Some
import components.MagicMergeResult


trait MagicMergeComponentImpl extends MagicMergeComponent {
  this: MagicMergeComponentImpl
    with BranchRepositoryComponent
    with GithubServiceComponent
    with TargetprocessComponent
  =>

  val magicMergeService: MagicMergeService = new MagicMergeServiceImpl


  class MagicMergeServiceImpl extends MagicMergeService {
    override def merge(branchName: String, user:User): Try[MagicMergeResult] = {

      val branches = branchRepository

      branches.getBranch(branchName) match {
        case None => Failure(new NotFoundException(s"Branch $branchName is not found"))
        case Some(branch) =>
          branch.pullRequest match {

            case None => Failure(new NotFoundException(s"There is no pull request for branch $branchName"))
            case Some(pullRequest) => Success(mergeAndDelete(branch, pullRequest, user))
          }
      }
    }


    def log(merged: Try[MergeResult], deleted: Try[Unit], closed: Try[Option[EntityState]]) = {
      merged.recover {
        case e => play.Logger.error("Error during merge", e)
      }
      deleted.recover {
        case e => play.Logger.error("Error during delete", e)
      }
      closed match {
        case Success(None) => play.Logger.warn("There is no final state")
        case Failure(e) => play.Logger.error("Error during closing entity")
        case _ =>
      }
    }

    private def mergeAndDelete(branch: Branch, pullRequest: PullRequest, user:User): MagicMergeResult = {

      val tryMerge = Try {
        githubService.mergePullRequest(pullRequest.prId, user)
      }
      val tryDelete = tryMerge.flatMap(_ => Try {
        githubService.deleteBranch(branch.name)
      })


      val tryChangeState: Try[Option[EntityState]] = tryDelete match {
        case e@Failure(ex) => Failure(ex)
        case Success(_) =>
          val pair: Option[(Entity, EntityState)] = for (
            entity <- branch.entity;
            finalState <- entity.state.nextStates.find(_.isFinal)
          ) yield (entity, finalState)

          pair match {
            case None => Success(None)
            case Some((entity, finalState)) =>
              val result: Try[Option[EntityState]] = Try {
                Some(entityRepository.changeEntityState(entity.id, finalState.id))
              }
              result
          }
      }

      log(tryMerge, tryDelete, tryChangeState)


      MagicMergeResult(pullRequest,
        merged = tryMerge.map(_.isMerged).getOrElse(false),
        deleted = tryDelete.isSuccess,
        closed = tryChangeState.isSuccess
      )
    }
  }

}
