package workflow

package object flows {
  type Absent[+A]              = None.type
  type WorkflowCreationRequest = Workflow[Absent]
  type WorkflowWithId          = Workflow[Some]
}
