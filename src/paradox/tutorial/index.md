# AFABL: A Friendly Adaptive Behavior Language

AFABL is an internal domain-specific language (DSL) shallowly embedded in the Scala programming language. Here we present the basic elements of AFABL with running example: a Pac Man agent for simplified Pac Man game.

## The AFABL Language

AFABL is a DSL for implementing adaptive agents. An AFABL agent operates in a world, is composed of one or more modules, and has an agent level reward function that it uses to learn a command arbitration policy, that is, how to prioritize its behavior modules.

### Worlds

Every AFABL module and agent is designed to operate in a world. A world defines the states, actions and state transition dynamics for a given state and action types. Details are discussed below.

The **states** of a world can be represented with any kind of Scala class. Case classes are good for representing states because of their concise syntax and built-in equality methods. Here is a case class for a state with XXX state variables: the locations of Pac Man, the food pellets, and the ghost.

```Scala
case class Location(x: Int, y: Int)

case class PacManState(
  pacMan: Location,
  ghost: Location,
  food: Seq[Location],
  cherries: Seq[Location],
  cherryActive: Boolean
)
```

**Actions** are also represented by objects which can be instances of any class. As with states, case classes make a good choice for implementing actions. Here we show actions for the Pac Man agent implemented as a Scala enumeration.

```Scala
object PacManAction extends Enumeration {
  type PacManAction = Value
  val Up, Down, Left, Right = Value
}
```

#### World Dynamics

An agent executes actions in a world, and those actions potentially change the state of the world. Having defined Scala representations for states and actions, we can define a world. Below is the abstract class which defines the basic interface of world objects, which are instances of subclasses if `World`. All modules and agents are defined to act in a particular instance of a world. As with states and actions, world representations make no advanced use of the Scala programming language.

```Scala
abstract class World[S, A] {
  def init(): S
  def resetAgent(): S
  def states: Seq[S]
  def actions: Seq[A]
  def act(action: A): S
}
```

Here are the important portions of the Pac Man world with explanatory comments.

```Scala
class PacManWorld extends World[PacManState, PacManAction] {

  def init(): S = {
    // Put PacMan, and the ghost in their start states,
    // place all the food pellets and cherries
  }

  def resetAgent(): S = {
    // "Respawn" the PacMan back in its start state
  }

  // In Scala you can override a def with a val
  val states: Seq[S] = {
    // All the possible states of the PacManWorld
    // Needed by reinforcement learning algorithms
    // Yes, this is huge, but it's computed only once.
  }

  def actions: Seq[A] = {
    // All the possible actions PacMan can execute in the PacManWorld
    // Needed by reinforcement learning algorithms
  }
  def act(action: A): S = {
    // Execute PacMan's action, update the state of the world including
    // PacMan's movement, the ghost's movement, disappearing food pellets,
    // and possibly cherryActive. Return the updated state.
  }
}
```

## Modules

Below is the complete code for an AFABL implementation of a behavior module that represents the goal of finding food.

```Scala
case class FindFoodState(pacMan: Location, food: Seq[Location])

val findFood = AfablModule(
  world = new PacManWorld,

  stateAbstraction = (worldState: PacManState) => {
    FindFoodState(worldState.pacMan, worldState.food)
  },

  moduleReward = (moduleState: FindFoodState) => {
    if (moduleState.food.contains(moduleState.pacMan)) 1.0
    else -0.1
  }
)
```

Let's consider each element of the definition above. First is the definition of a case class, `FindFoodState`, to represent the state abstraction for `FindFood` modules. `FindFoodState` includes only two of the three state variables in the bunny world (yes, `food` is a `Seq`, but we dont' have to think of the food pellets individually).

```Scala
case class FindFoodState(pacMan: Location, food: Seq[Location])
```

We will use this state abstraction class in the `stateAbstraction` function below.

We store a reference to an `AfablModule` for FindFood in `findFood`.

```Scala
val findFood = AfablModule(
```

The `AfablModule` factory method takes three arguments: an instance of a `World` that the module can act and learn in, a `stateAbstraction` function, and a `moduleReward` function.

The first argument to `AfablModule` is the world:

```Scala
world = new PacManWorld
```

The `world` and `=` must be verbatim, i.e., considered part of the AFABL language.

The second argument is a state abstraction function that takes a world-state object as a parameter and returns an instance of our state abstraction class:

```Scala
stateAbstraction = (worldState: PacManState) => {
  FindFoodState(worldState.pacMan, worldState.food)
}
```

The `stateAbstraction` and `=` must be verbatim, part of the AFABL language. `worldState` is a user-chosen name, `PacManState` must match the state type defined for the world in which the module and agent operate, in this case it is the first type parameter to `World` in the `PacManWorld` code above. The last expression in the body of the `stateAbstraction` function must be an instance of a module state, in this case `FindFoodState`.

The `stateAbstraction` function is optional. If you don't supply your own state abstraction function a default is provided: the identity function, i.e., no state abstraction. However, the learning time and performance of your modules will be better if you provide a state abstraction class and function.

The third and final argument to the `AfablModule` factory method is a module reward function that takes an instance of our state abstraction class and returns the reward this module receives for being in that state:

```Scala
moduleReward = (moduleState: FindFoodState) => {
  if (moduleState.food.contains(moduleState.pacMan)) 1.0
  else -0.1
}
```

The `moduleReward` and `=` are part of the AFABL language. `moduleState` is a user-chosen name, but the parameter type, `FindFoodState` in this example, must match the return type of the `stateAbstraction` function (or the world state, `PacManState` if you chose not to create a state abstraction class and function). The last expression in the body of the `moduleReward` function must be a `Double` value. In this case, which is typical, the body of the `moduleReward` function is an `if` expression which simply returns the reward based on state predicates. This example is another case where we could have implemented DSL-specific syntax, such as a list of predicates and values, but the syntactic overhead of Scala's `if` expression is minimal and the code is crystal clear to any Scala programmer.

This `moduleReward` function gives the agent a reward of `1.0` for finding each food pellet and `-0.1` for each time step in which the Pac Man does not eat. This is a technique in reward authoring: there should be a small negative reward for not moving toward a goal state, in this case finding food pellets. Also note `moduleReward` is not the same as a score. The job of the `moduleRewared` function is to guide the behavior of this behavior module of the Pac Man agent. It is a declarative specificaiton of behavior: it specifies which states are "good" and which states are "bad". AFABL uses this infomation to derive a control policy: given the state, whcih action should the Pac Man agent execute to maximize its reward.

These three components -- world, state abstraction and module reward -- define a module specific learning problem on a subset of the world in which the module (and agent containing the module) may act. Each module is selfish, ignoring any other behavior modules or goasl the agent may have, such as avoiding ghosts. In addition to `FindFood` we would create behavior modules for each of the other goals the Pac Man agent must continuously pursue.


```Scala
case class AvoidGhostState(pacMan: Location, ghost: Location)

val avoidGhost = AfablModule(
  world = new AvoidGhostWorld,

  stateAbstraction = (worldState: AvoidGhostState) => {
    AvoidGhostState(worldState.pacMan, worldState.ghost)
  },

  moduleReward = (moduleState: AvoidGhostState) => {
    if (moduleState.pacMan == moduleState.ghost) -1.0
    else 0.5
  }
)
```

```Scala
case class FindCherriesState(pacMan: Location, cherries: Seq[Location])

val findCherries = AfablModule(
  world = new PacManWorld,

  stateAbstraction = (worldState: PacManState) => {
    FindCherriesState(worldState.pacMan, worldState.cherries)
  },

  moduleReward = (moduleState: FindCherriesState) => {
    if (moduleState.cherries.contains(moduleState.pacMan)) 1.0
    else -0.1
  }
)
```
