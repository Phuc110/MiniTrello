import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DragDropContext, Droppable } from "@hello-pangea/dnd";
import type { ReactElement } from "react";
import { TaskCard } from "./TaskCard";
import type { Task } from "@/types";

/**
 * TaskCard renders as a @hello-pangea/dnd Draggable, which requires a
 * DragDropContext + Droppable ancestor to mount without throwing — this
 * harness provides the minimum viable one for a render-only test (no
 * actual drag simulation, which the library's own test suite already
 * covers).
 */
function renderInDndContext(ui: ReactElement) {
  return render(
    <DragDropContext onDragEnd={() => {}}>
      <Droppable droppableId="test-column">
        {(provided) => (
          <div ref={provided.innerRef} {...provided.droppableProps}>
            {ui}
            {provided.placeholder}
          </div>
        )}
      </Droppable>
    </DragDropContext>,
  );
}

const baseTask: Task = {
  id: "task-1",
  boardListId: "list-1",
  title: "Fix login bug",
  description: null,
  priority: "HIGH",
  position: "m",
  dueDate: null,
  assignees: [],
  tags: [],
  createdAt: new Date().toISOString(),
};

describe("TaskCard", () => {
  it("renders the task title", () => {
    renderInDndContext(<TaskCard task={baseTask} index={0} onClick={() => {}} />);
    expect(screen.getByText("Fix login bug")).toBeInTheDocument();
  });

  it("calls onClick when clicked", async () => {
    const onClick = vi.fn();
    renderInDndContext(<TaskCard task={baseTask} index={0} onClick={onClick} />);
    await userEvent.click(screen.getByText("Fix login bug"));
    expect(onClick).toHaveBeenCalledOnce();
  });

  it("renders each tag with its own color", () => {
    const taskWithTags: Task = {
      ...baseTask,
      tags: [{ id: "tag-1", name: "Bug", color: "#EF4444" }],
    };
    renderInDndContext(<TaskCard task={taskWithTags} index={0} onClick={() => {}} />);
    const tag = screen.getByText("Bug");
    expect(tag).toHaveStyle({ backgroundColor: "#EF4444" });
  });

  it("shows initials for up to 3 assignees", () => {
    const taskWithAssignees: Task = {
      ...baseTask,
      assignees: [
        { userId: "u1", fullName: "Alice Smith", email: "alice@example.com" },
        { userId: "u2", fullName: "Bob Jones", email: "bob@example.com" },
      ],
    };
    renderInDndContext(<TaskCard task={taskWithAssignees} index={0} onClick={() => {}} />);
    expect(screen.getByTitle("Alice Smith")).toHaveTextContent("A");
    expect(screen.getByTitle("Bob Jones")).toHaveTextContent("B");
  });
});
