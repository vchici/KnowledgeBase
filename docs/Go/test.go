package main

import "fmt"

type Student struct {
	Name string
	Age  int
}

func (s *Student) addAge() int {
	s.Age++
	return s.Age
}

func main() {
	var s Student = Student{Name: "a", Age: 20}
	fmt.Println(s.addAge())
}
