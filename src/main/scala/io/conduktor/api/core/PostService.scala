package io.conduktor.api.core

import eu.timepit.refined.types.string.NonEmptyString
import io.conduktor.api.auth.UserAuthenticationLayer.User
import io.conduktor.api.core.Post.{Content, Title}
import io.conduktor.api.db.repository.PostRepository
import io.estatico.newtype.macros.newtype
import zio.{Function1ToLayerSyntax, Has, Task, URLayer, ZIO}

import java.util.UUID

final case class Post(
  id: UUID,
  title: NonEmptyString,
  author: User,
  published: Boolean,
  content: String
)
object Post {
  @newtype case class Title(value: NonEmptyString)
  @newtype case class Content(value: String)
}

trait PostService {
  def createPost(user: User, title: Title, content: Content): Task[Post]

  def deletePost(uuid: UUID): Task[Unit]

  def findById(uuid: UUID): Task[Post]

  def all: Task[List[Post]]
}

final class PostServiceLive(db: PostRepository) extends PostService {

  override def createPost(user: User, title: Title, content: Content): Task[Post] =
    for {
      maybePost <- db.findPostByTitle(title)
      post      <- maybePost match {
                     case Some(post) => ZIO.succeed(post)
                     case None       => db.createPost(UUID.randomUUID(), title, user.name, content)
                   }
    } yield post

  override def deletePost(id: UUID): Task[Unit] = db.deletePost(id)

  override def findById(id: UUID): Task[Post] = db.findPostById(id)

  override def all: Task[List[Post]] = db.allPosts
}

object PostServiceLive {
  val layer: URLayer[Has[PostRepository], Has[PostService]] = (new PostServiceLive(_)).toLayer
}
