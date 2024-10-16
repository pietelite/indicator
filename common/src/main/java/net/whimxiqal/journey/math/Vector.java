/*
 * MIT License
 *
 * Copyright (c) whimxiqal
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.whimxiqal.journey.math;

import java.util.Objects;

public record Vector(double x, double y, double z) {

  public Vector subtract(Vector other) {
    return new Vector(x - other.x, y - other.y, z - other.z);
  }

  public Vector add(Vector other) {
    return new Vector(x + other.x, y + other.y, z + other.z);
  }

  public Vector times(double factor) {
    return new Vector(x * factor, y * factor, z * factor);
  }

  public double projectionOnto(Vector other) {
    return dot(other) / other.magnitude();
  }

  public double dot(Vector other) {
    return x * other.x + y * other.y + z * other.z;
  }

  public Vector cross(Vector other) {
    return new Vector(this.y * other.z - this.z * other.y,
        this.z * other.x - this.x * other.z,
        this.x * other.y - this.y * other.x);
  }

  public double magnitude() {
    return Math.sqrt(magnitudeSquared());
  }

  public double magnitudeSquared() {
    return (x * x) + (y * y) + (z * z);
  }

  public Vector unit() {
    return times(1d / magnitude());
  }

  public Vector min(Vector other) {
    return new Vector(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
  }

  public Vector max(Vector other) {
    return new Vector(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
  }

  @Override
  public String toString() {
    return "Vector{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Vector vector = (Vector) o;
    return Double.compare(vector.x, x) == 0 && Double.compare(vector.y, y) == 0 && Double.compare(vector.z, z) == 0;
  }

}
