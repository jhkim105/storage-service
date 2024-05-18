package jhkim105.storage.controller;

public record ResizeRequest(
    Integer w, Integer h
) {

  public ResizeRequest(Integer w, Integer h) {
    this.w = (w != null) ? w : h;
    this.h = (h != null) ? h : w;
  }
  public boolean hasValue() {
    return w != null || h != null;
  }
}
