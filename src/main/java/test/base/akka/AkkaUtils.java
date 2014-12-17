/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.base.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import java.util.List;
import java.util.concurrent.TimeUnit;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 *
 * @author Gwen
 */
public class AkkaUtils {

    public static Object ask(ActorRef actor, Object message, long timeout) throws Exception {
        Future<Object> future = Patterns.ask(actor, message, timeout);
        return Await.result(future, Duration.create(timeout, TimeUnit.MILLISECONDS));
    }

    public static Object ask(ActorSelection actor, Object message, long timeout) throws Exception {
        Future<Object> future = Patterns.ask(actor, message, timeout);
        return Await.result(future, Duration.create(timeout, TimeUnit.MILLISECONDS));
    }

    public static boolean myRole(ActorSystem system, String role) {
        List<String> roles = system.settings().config().getStringList("akka.cluster.roles");
        return roles.stream().anyMatch((str) -> (role.equals(str)));
    }
}
