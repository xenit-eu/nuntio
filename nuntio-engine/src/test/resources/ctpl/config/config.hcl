consul {
  address = "consul:8500"
}

template {
  source      = "/templates/test.ctmpl"
  destination = "/output/test.txt"
}
